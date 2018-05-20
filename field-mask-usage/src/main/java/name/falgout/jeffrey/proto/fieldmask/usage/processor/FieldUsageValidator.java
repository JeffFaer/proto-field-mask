package name.falgout.jeffrey.proto.fieldmask.usage.processor;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.MoreCollectors.toOptional;
import static java.util.stream.Collectors.toMap;
import static name.falgout.jeffrey.proto.fieldmask.FieldMask.toFieldMask;

import com.google.auto.value.AutoValue;
import com.google.common.base.CaseFormat;
import com.google.common.base.Preconditions;
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
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import name.falgout.jeffrey.proto.ProtoDescriptor;
import name.falgout.jeffrey.proto.fieldmask.FieldMask;
import name.falgout.jeffrey.proto.fieldmask.FieldPath;
import name.falgout.jeffrey.proto.fieldmask.usage.RequiresFields;
import name.falgout.jeffrey.proto.fieldmask.usage.processor.FieldUsageValidator.FieldMaskWithSource.Source;
import name.falgout.jeffrey.proto.fieldmask.usage.processor.RequiresFieldsValidator.RequiresFieldsProcessingStep;

@BugPattern(
    name = "ProtoFieldUsageValidator",
    summary = "Validates field access for protobuf instances annotated with @RequiresFields.",
    severity = SeverityLevel.ERROR
)
public final class FieldUsageValidator extends BugChecker implements MethodTreeMatcher {
  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    Map<Name, FieldMaskWithSource> fieldMasks =
        tree.getParameters()
            .stream()
            .map(var ->
                getRequiredFields(ASTHelpers.getSymbol(var))
                    .map(FieldMaskWithSource::createFromAnnotation)
                    .map(fieldMask -> new SimpleImmutableEntry<>(var.getName(), fieldMask)))
            .flatMap(Streams::stream)
            .collect(toMap(Entry::getKey, Entry::getValue));

    if (fieldMasks.isEmpty()) {
      return Description.NO_MATCH;
    }

    new FieldMaskUsageVisitor(state).scan(tree.getBody(), fieldMasks).forEach(state::reportMatch);
    return Description.NO_MATCH;
  }

  private static Optional<FieldMask<?>> getRequiredFields(Symbol sym) {
    RequiresFields requiresFields = sym.getAnnotation(RequiresFields.class);
    if (requiresFields == null) {
      return Optional.empty();
    }

    ProtoDescriptor<?> descriptor = getDescriptor(sym.asType());
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
      TreeScanner<ImmutableList<Description>, Map<Name, FieldMaskWithSource>> {
    private final VisitorState state;

    FieldMaskUsageVisitor(VisitorState state) {
      this.state = state;
    }

    @Override
    public ImmutableList<Description> scan(Tree tree, Map<Name, FieldMaskWithSource> fieldMasks) {
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
    public ImmutableList<Description> visitVariable(
        VariableTree node, Map<Name, FieldMaskWithSource> fieldMasks) {
      VarSymbol variable = ASTHelpers.getSymbol(node);
      if (hasValidRequiresFields(variable)) {
        getRequiredFields(variable)
            .map(FieldMaskWithSource::createFromAnnotation)
            .ifPresent(fieldMask -> fieldMasks.put(node.getName(), fieldMask));
      }

      if (node.getInitializer() == null) {
        return ImmutableList.of();
      }

      return visitAssignment(node, node.getName(), node.getInitializer(), fieldMasks);
    }

    /**
     * Validates that the given {@code element} has a valid {@code @RequiresFields} annotation.
     *
     * Local variable annotations aren't checked by Annotation Processors, so we have to do it here,
     * just in case.
     */
    private boolean hasValidRequiresFields(Element symbol) {
      if (symbol.getAnnotation(RequiresFields.class) == null) {
        return false;
      }

      ProcessingEnvironment processingEnv = JavacProcessingEnvironment.instance(state.context);
      RequiresFieldsProcessingStep step = new RequiresFieldsProcessingStep(processingEnv);
      try {
        return step.validateRequiresFields(symbol);
      } catch (ClassNotFoundException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    public ImmutableList<Description> visitAssignment(
        AssignmentTree node, Map<Name, FieldMaskWithSource> fieldMasks) {
      if (!(node.getVariable() instanceof IdentifierTree)) {
        return ImmutableList.of();
      }

      IdentifierTree variable = (IdentifierTree) node.getVariable();
      return visitAssignment(node, variable.getName(), node.getExpression(), fieldMasks);
    }

    private ImmutableList<Description> visitAssignment(
        Tree node,
        Name variableName,
        ExpressionTree initializer,
        Map<Name, FieldMaskWithSource> fieldMasks) {
      Optional<FieldMaskWithSource> newFieldMask =
          FieldMaskRetriever.INSTANCE
              .scan(initializer, fieldMasks)
              .map(FieldMaskWithSource::createFromAssignment);

      if (fieldMasks.containsKey(variableName)) {
        if (!newFieldMask.isPresent()) {
          return ImmutableList.of(
              buildDescription(node)
                  .setMessage("Cannot assign RHS without FieldMask.")
                  .build());
        }

        FieldMaskWithSource originalFieldMask = fieldMasks.get(variableName);
        if (originalFieldMask.getSource() == Source.ANNOTATION) {
          if (!containsAll(newFieldMask.get().getFieldMask(), originalFieldMask.getFieldMask())) {
            return ImmutableList.of(
                buildDescription(node)
                    .setMessage("RHS has incompatible FieldMask.")
                    .build());
          }

          // Don't override the annotation's FieldMask.
          return ImmutableList.of();
        }
      }

      newFieldMask.ifPresent(fieldMask -> fieldMasks.put(variableName, fieldMask));
      return ImmutableList.of();
    }

    @Override
    public ImmutableList<Description> visitMemberSelect(
        MemberSelectTree node, Map<Name, FieldMaskWithSource> fieldMasks) {
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

    @Override
    public ImmutableList<Description> visitMethodInvocation(
        MethodInvocationTree node, Map<Name, FieldMaskWithSource> fieldMasks) {
      MethodSymbol method = ASTHelpers.getSymbol(node);
      // TODO: varargs
      Preconditions.checkState(method.getParameters().size() == node.getArguments().size());

      ImmutableList.Builder<Description> descriptions = ImmutableList.builder();

      for (int i = 0; i < method.getParameters().size(); i++) {
        VarSymbol parameter = method.getParameters().get(i);
        ExpressionTree argument = node.getArguments().get(i);

        Optional<FieldMask<?>> expectedFieldMask = getRequiredFields(parameter);
        if (!expectedFieldMask.isPresent()) {
          continue;
        }

        Optional<FieldMask<?>> actualFieldMask =
            FieldMaskRetriever.INSTANCE.scan(argument, fieldMasks);
        if (!actualFieldMask.isPresent()) {
          continue;
        }

        if (!containsAll(actualFieldMask.get(), expectedFieldMask.get())) {
          // TODO: Better error message.
          descriptions.add(
              buildDescription(argument)
                  .setMessage("Argument has incompatible FieldMask.")
                  .build());
        }
      }

      return reduce(super.visitMethodInvocation(node, fieldMasks), descriptions.build());
    }
  }

  private static <M extends Message> boolean containsAll(
      FieldMask<M> haystack, FieldMask<?> needle) {
    return haystack.containsAll(needle.castTo(haystack.getDescriptorForType()));
  }

  private static class FieldMaskRetriever
      extends TreeScanner<Optional<FieldMask<?>>, Map<Name, FieldMaskWithSource>> {
    private static final FieldMaskRetriever INSTANCE = new FieldMaskRetriever();

    @Override
    public Optional<FieldMask<?>> scan(Tree tree, Map<Name, FieldMaskWithSource> fieldMasks) {
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
        IdentifierTree node, Map<Name, FieldMaskWithSource> fieldMasks) {
      return Optional.ofNullable(fieldMasks.get(node.getName()))
          .map(FieldMaskWithSource::getFieldMask);
    }

    @Override
    public Optional<FieldMask<?>> visitMemberSelect(
        MemberSelectTree node, Map<Name, FieldMaskWithSource> fieldMasks) {
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

  @AutoValue
  abstract static class FieldMaskWithSource {
    static FieldMaskWithSource createFromAnnotation(FieldMask<?> fieldMask) {
      return new AutoValue_FieldUsageValidator_FieldMaskWithSource(fieldMask, Source.ANNOTATION);
    }

    static FieldMaskWithSource createFromAssignment(FieldMask<?> fieldMask) {
      return new AutoValue_FieldUsageValidator_FieldMaskWithSource(fieldMask, Source.ASSIGNMENT);

    }

    enum Source {
      ANNOTATION,
      ASSIGNMENT,
    }

    abstract FieldMask<?> getFieldMask();

    abstract Source getSource();
  }
}
