package name.falgout.jeffrey.proto.fieldmask;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Message;
import java.util.Objects;
import java.util.Optional;

final class AllowAllFieldMask<M extends Message> extends FieldMask<M> {
  private final Descriptor descriptor;

  AllowAllFieldMask(Descriptor descriptor) {
    this.descriptor = descriptor;
  }

  @Override
  public Descriptor getDescriptorForType() {
    return descriptor;
  }

  @Override
  boolean doContains(FieldPath<?> path) {
    return true;
  }

  @Override
  FieldMask<?> doGetSubFieldMask(FieldPath<?> path) {
    return new AllowAllFieldMask<>(path.getLastField().getMessageType());
  }

  @Override
  public Optional<com.google.protobuf.FieldMask> toProto() {
    return Optional.empty();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AllowAllFieldMask)) {
      return false;
    }
    AllowAllFieldMask<?> that = (AllowAllFieldMask<?>) o;
    return Objects.equals(descriptor, that.descriptor);
  }

  @Override
  public int hashCode() {
    return Objects.hash(descriptor);
  }
}
