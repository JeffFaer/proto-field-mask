package name.falgout.jeffrey.proto.fieldmask;

import com.google.common.collect.Iterables;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Message;
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
    return new AllowAllFieldMask<>(Iterables.getLast(path.getPath()).getMessageType());
  }

  @Override
  public Optional<com.google.protobuf.FieldMask> toProto() {
    return Optional.empty();
  }
}
