package name.falgout.jeffrey.proto.fieldmask;

import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.DoNotMock;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.Message;
import java.util.Optional;

@DoNotMock
public abstract class FieldMask<M extends Message> {
  public static <M extends Message> FieldMask<M> allowAll(M prototype) {
    return new AllowAllFieldMask<>(prototype.getDescriptorForType());
  }

  FieldMask() {}

  /** The {@link Descriptor} for {@code M} */
  public abstract Descriptor getDescriptorForType();

  /**
   * Determines whether the given {@link FieldPath} is included in this {@code FieldMask}.
   *
   * @throws IllegalArgumentException if the {@code path}'s {@linkplain
   *     FieldPath#getDescriptorForType() type} is not equal to this {@code FieldMask}'s {@linkplain
   *     #getDescriptorForType() type}
   */
  public boolean contains(FieldPath<?> path) {
    Preconditions.checkArgument(path.getDescriptorForType().equals(getDescriptorForType()));
    return doContains(path);
  }

  abstract boolean doContains(FieldPath<?> path);

  /**
   * Returns a {@code FieldMask} for the given {@code FieldPath}.
   *
   * @throws IllegalArgumentException if the {@code path}'s {@linkplain
   *     FieldPath#getDescriptorForType() type} is not equal to this {@code FieldMask}'s {@linkplain
   *     #getDescriptorForType() type} or if the {@code path} does not {@linkplain
   *     FieldPath#getLastField() terminate} in a {@linkplain JavaType#MESSAGE message}
   */
  public FieldMask<?> getSubFieldMask(FieldPath<?> path) {
    Preconditions.checkArgument(path.getDescriptorForType().equals(getDescriptorForType()));
    Preconditions.checkArgument(path.getLastField().getJavaType() == JavaType.MESSAGE);

    return doGetSubFieldMask(path);
  }

  abstract FieldMask<?> doGetSubFieldMask(FieldPath<?> path);

  public abstract Optional<com.google.protobuf.FieldMask> toProto();

  /** A builder for {@code FieldMask}s. */
  public abstract static class Builder<M extends Message> {
    Builder() {}

    /**
     * Add the specified {@code field} to this {@code Builder}.
     *
     * If the {@code field} is a {@linkplain JavaType#MESSAGE message}, all of its sub-fields will
     * be recursively included.
     */
    public abstract Builder<M> addField(FieldDescriptor field);

    /**
     * Add the specified {@code field} to this {@code Builder} with the given {@code subFieldMask}.
     *
     * @throws IllegalArgumentException if the {@code field}'s {@linkplain
     *     FieldDescriptor#getMessageType() type} does not match the {@code subFieldMask}'s
     *     {@linkplain FieldMask#getDescriptorForType() type}.
     */
    public abstract Builder<M> addField(FieldDescriptor field, FieldMask<?> subFieldMask);

    public abstract FieldMask<M> build();
  }
}
