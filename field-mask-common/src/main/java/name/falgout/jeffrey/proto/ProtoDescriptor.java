package name.falgout.jeffrey.proto;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Internal;
import com.google.protobuf.Message;

/** A type-safe {@link Descriptor}. */
@AutoValue
public abstract class ProtoDescriptor<M extends Message> {
  public static <M extends Message> ProtoDescriptor<M> create(Class<M> type) {
    return new AutoValue_ProtoDescriptor<>(
        Internal.getDefaultInstance(type).getDescriptorForType());
  }

  public static ProtoDescriptor<?> create(Descriptor descriptor) {
    return new AutoValue_ProtoDescriptor<>(descriptor);
  }

  ProtoDescriptor() {}

  public abstract Descriptor getDescriptorForType();

  /**
   * Safely casts this {@code ProtoDescriptor} to the specified {@code type}.
   *
   * This is useful if you have a {@code ProtoDescriptor<?>} and you want to reify its generic
   * parameter.
   */
  public final <N extends Message> ProtoDescriptor<N> castTo(Class<N> type) {
    return castTo(ProtoDescriptor.create(type));
  }

  /**
   * Safely casts this {@code ProtoDescriptor} to {@code N}.
   *
   * This is useful if you have a {@code ProtoDescriptor<?>} and you want to reify its generic
   * parameter.
   */
  public final <N extends Message> ProtoDescriptor<N> castTo(ProtoDescriptor<N> descriptor) {
    return castTo(descriptor.getDescriptorForType());
  }

  private <N extends Message> ProtoDescriptor<N> castTo(Descriptor descriptor) {
    Preconditions.checkArgument(
        descriptor.equals(getDescriptorForType()), // N == M
        "Type mismatch. %s != %s",
        descriptor,
        getDescriptorForType());

    @SuppressWarnings("unchecked")
    ProtoDescriptor<N> result = (ProtoDescriptor<N>) this;
    return result;
  }

  @Override
  public String toString() {
    return "ProtoDescriptor{"
        + getDescriptorForType().getFullName()
        + "}";
  }
}
