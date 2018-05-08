package name.falgout.jeffrey.proto.fieldmask;

import static java.util.stream.Collectors.joining;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.Internal;
import com.google.protobuf.Message;
import com.google.protobuf.util.FieldMaskUtil;
import java.util.stream.Stream;

/**
 * A {@code FieldPath} represents a single {@linkplain com.google.protobuf.FieldMask#getPathsList()
 * path} in a {@link com.google.protobuf.FieldMask}.
 *
 * Unlike the paths in {@code FieldMask}, these paths can be empty. An empty {@code FieldPath}
 * allows the entire message when used in a {@link FieldMask}.
 */
@AutoValue
public abstract class FieldPath<M extends Message> {
  private static final String FIELD_PATH_SEPARATOR = ".";
  private static final Splitter FIELD_PATH_SPLITTER = Splitter.on(FIELD_PATH_SEPARATOR);

  /**
   * Creates a new {@code FieldPath} from the given path string.
   *
   * @throws IllegalArgumentException if the path string is invalid for the given proto type
   */
  public static <M extends Message> FieldPath<M> create(Class<M> type, String pathString) {
    Preconditions.checkArgument(FieldMaskUtil.isValid(type, pathString), "Invalid pathString");

    ImmutableList.Builder<FieldDescriptor> fields = ImmutableList.builder();
    Descriptor descriptor = Internal.getDefaultInstance(type).getDescriptorForType();

    FieldDescriptor lastField = null;
    for (String fieldName : FIELD_PATH_SPLITTER.split(pathString)) {
      Descriptor lastDescriptor = lastField == null ? descriptor : lastField.getMessageType();

      lastField = lastDescriptor.findFieldByName(fieldName);
      fields.add(lastField);
    }

    return create(type, fields.build());
  }

  /**
   * Creates a new {@code FieldPath} from the given {@code FieldDescriptor}s.
   *
   * @throws IllegalArgumentException if the path is invalid for the given proto type
   */
  public static <M extends Message> FieldPath<M> create(Class<M> type, FieldDescriptor... fields) {
    return create(Internal.getDefaultInstance(type).getDescriptorForType(), fields).castTo(type);
  }

  static FieldPath<?> create(Descriptor descriptor, FieldDescriptor... fields) {
    if (fields.length == 0) {
      return create(descriptor, ImmutableList.of());
    }

    String pathString =
        Stream.of(fields).map(FieldDescriptor::getName).collect(joining(FIELD_PATH_SEPARATOR));
    Preconditions.checkArgument(FieldMaskUtil.isValid(descriptor, pathString));

    return create(descriptor, ImmutableList.copyOf(fields));
  }

  private static <M extends Message> FieldPath<M> create(
      Class<M> type,
      Iterable<? extends FieldDescriptor> fields) {
    return new AutoValue_FieldPath<>(
        Internal.getDefaultInstance(type).getDescriptorForType(), ImmutableList.copyOf(fields));
  }

  private static FieldPath<?> create(
      Descriptor descriptor,
      Iterable<? extends FieldDescriptor> fields) {
    return new AutoValue_FieldPath<>(descriptor, ImmutableList.copyOf(fields));
  }

  /** Creates a new {@code FieldPath} by appending {@code tail} to the {@code fieldPath}. */
  public static <M extends Message> FieldPath<M> append(
      FieldPath<M> fieldPath,
      FieldDescriptor tail) {
    if (fieldPath.getPath().isEmpty()) {
      return create(fieldPath.getDescriptorForType(), tail).castTo(fieldPath);
    }

    Preconditions.checkArgument(fieldPath.getLastField().getJavaType() == JavaType.MESSAGE);
    Preconditions.checkArgument(
        fieldPath.getLastField().getMessageType().equals(tail.getContainingType()));

    ImmutableList.Builder<FieldDescriptor> newPath =
        ImmutableList.builderWithExpectedSize(fieldPath.getPath().size() + 1);
    newPath.addAll(fieldPath.getPath());
    newPath.add(tail);

    return create(fieldPath.getDescriptorForType(), newPath.build()).castTo(fieldPath);
  }

  FieldPath() {}

  /** The descriptor for {@code M}. */
  public abstract Descriptor getDescriptorForType();

  /** The fields that form this {@code FieldPath}. */
  public abstract ImmutableList<FieldDescriptor> getPath();

  public final FieldDescriptor getLastField() {
    return Iterables.getLast(getPath());
  }

  public final String toPathString() {
    return getPath().stream().map(FieldDescriptor::getName).collect(joining(FIELD_PATH_SEPARATOR));
  }

  /**
   * Safely casts this {@code FieldPath} to the specified {@code type}.
   *
   * This is useful if you have a {@code FieldPath<?>} and you want to reify its generic parameter.
   */
  public final <N extends Message> FieldPath<N> castTo(Class<N> type) {
    return castTo(Internal.getDefaultInstance(type).getDescriptorForType());
  }

  private <N extends Message> FieldPath<N> castTo(FieldPath<N> other) {
    return castTo(other.getDescriptorForType());
  }

  private <N extends Message> FieldPath<N> castTo(Descriptor descriptor) {
    Preconditions.checkArgument(
        descriptor.equals(getDescriptorForType()),
        "Type mismatch. %s != %s",
        descriptor,
        getDescriptorForType());

    @SuppressWarnings("unchecked")
    FieldPath<N> result = (FieldPath<N>) this;
    return result;
  }
}
