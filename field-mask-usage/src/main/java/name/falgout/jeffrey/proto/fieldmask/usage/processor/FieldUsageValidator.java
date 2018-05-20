package name.falgout.jeffrey.proto.fieldmask.usage.processor;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.MoreCollectors.toOptional;
import static java.util.stream.Collectors.toMap;
import static name.falgout.jeffrey.proto.fieldmask.FieldMask.toFieldMask;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.Message;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Type;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import javax.lang.model.element.Name;
import name.falgout.jeffrey.proto.ProtoDescriptor;
import name.falgout.jeffrey.proto.fieldmask.FieldMask;
import name.falgout.jeffrey.proto.fieldmask.FieldPath;
import name.falgout.jeffrey.proto.fieldmask.usage.RequiresFields;

@BugPattern(
    name = "ProtoFieldUsageValidator",
    summary = "Validates field access for protobuf instances annotated with @RequiresFields.",
    severity = SeverityLevel.ERROR
)
public final class FieldUsageValidator extends BugChecker implements MethodTreeMatcher {
  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    Map<Name, FieldMask<?>> fieldMasks =
        tree.getParameters()
            .stream()
            .map(var ->
                getFieldMask(var)
                    .map(fieldMask -> new SimpleImmutableEntry<>(var.getName(), fieldMask)))
            .flatMap(Streams::stream)
            .collect(toMap(Entry::getKey, Entry::getValue));

    if (fieldMasks.isEmpty()) {
      return Description.NO_MATCH;
    }

    new FieldMaskUsageVisitor().scan(tree.getBody(), fieldMasks).forEach(state::reportMatch);
    return Description.NO_MATCH;
  }

  private static Optional<FieldMask<?>> getFieldMask(Tree tree) {
    Type type = ASTHelpers.getType(tree);
    if (type == null) {
      return Optional.empty();
    }

    RequiresFields requiresFields = ASTHelpers.getAnnotation(tree, RequiresFields.class);
    if (requiresFields == null) {
      return Optional.empty();
    }

    ProtoDescriptor<?> descriptor = getDescriptor(type);
    return Optional.of(Stream.of(requiresFields.value()).collect(toFieldMask(descriptor)));
  }

  private static ProtoDescriptor<?> getDescriptor(Type type) {
    Name binaryName = type.asElement().flatName();
    try {
      Class<? extends Message> clazz =
          Class.forName(binaryName.toString()).asSubclass(Message.class);
      return ProtoDescriptor.create(clazz);
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException(e);
    }
  }

  private class FieldMaskUsageVisitor extends
      TreeScanner<ImmutableList<Description>, Map<Name, FieldMask<?>>> {
    @Override
    public ImmutableList<Description> scan(Tree tree, Map<Name, FieldMask<?>> fieldMasks) {
      ImmutableList<Description> result = super.scan(tree, fieldMasks);
      return result == null ? ImmutableList.of() : result;
    }

    @Override
    public ImmutableList<Description> reduce(
        ImmutableList<Description> r1, ImmutableList<Description> r2) {
      return Stream.of(r1, r2)
          .filter(Objects::nonNull)
          .flatMap(Collection::stream)
          .collect(toImmutableList());
    }

    @Override
    public ImmutableList<Description> visitMemberSelect(
        MemberSelectTree node, Map<Name, FieldMask<?>> fieldMasks) {
      Optional<FieldMask<?>> fieldMask =
          FieldMaskRetriever.INSTANCE.scan(node.getExpression(), fieldMasks);

      if (!fieldMask.isPresent()) {
        return scan(node.getExpression(), fieldMasks);
      }

      FieldMask<?> mask = fieldMask.get();
      String fieldName = toProtoFieldName(node.getIdentifier().toString());

      if (mask.contains(fieldName)) {
        return ImmutableList.of();
      }

      return ImmutableList.of(
          buildDescription(node)
              .setMessage("FieldMask does not contain " + fieldName)
              .build());
    }
  }

  private static class FieldMaskRetriever
      extends TreeScanner<Optional<FieldMask<?>>, Map<Name, FieldMask<?>>> {
    private static final FieldMaskRetriever INSTANCE = new FieldMaskRetriever();

    @Override
    public Optional<FieldMask<?>> scan(Tree tree, Map<Name, FieldMask<?>> fieldMasks) {
      Optional<FieldMask<?>> fieldMask = super.scan(tree, fieldMasks);
      return fieldMask == null ? Optional.empty() : fieldMask;
    }

    @Override
    public Optional<FieldMask<?>> reduce(Optional<FieldMask<?>> r1, Optional<FieldMask<?>> r2) {
      return Stream.of(r1, r2)
          .filter(Objects::nonNull)
          .flatMap(Streams::stream)
          .collect(toOptional());
    }

    @Override
    public Optional<FieldMask<?>> visitIdentifier(
        IdentifierTree node, Map<Name, FieldMask<?>> fieldMasks) {
      return Optional.ofNullable(fieldMasks.get(node.getName()));
    }

    @Override
    public Optional<FieldMask<?>> visitMemberSelect(
        MemberSelectTree node, Map<Name, FieldMask<?>> fieldMasks) {
      return scan(node.getExpression(), fieldMasks)
          .flatMap(fieldMask -> getSubFieldMask(fieldMask, node.getIdentifier()));
    }

    private static <M extends Message> Optional<FieldMask<?>> getSubFieldMask(
        FieldMask<M> fieldMask, Name memberSelect) {
      ProtoDescriptor<M> descriptor = fieldMask.getDescriptorForType();

      String fieldName = toProtoFieldName(memberSelect.toString());
      FieldDescriptor field = descriptor.getDescriptorForType().findFieldByName(fieldName);

      if (field == null || field.getJavaType() != JavaType.MESSAGE) {
        return Optional.empty();
      }

      FieldPath<M> fieldPath = FieldPath.create(descriptor, field);
      return Optional.of(fieldMask.getSubFieldMask(fieldPath));
    }
  }

  private static String toProtoFieldName(String methodName) {
    String lowerSnakeCase = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, methodName);
    return lowerSnakeCase.substring(lowerSnakeCase.indexOf('_') + 1);
  }
}
