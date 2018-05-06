package name.falgout.jeffrey.proto.fieldmask;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.Message;
import java.util.Optional;

@AutoValue
abstract class AllowSomeFieldMask<M extends Message> extends FieldMask<M> {
  @Override
  public abstract Descriptor getDescriptorForType();

  abstract ImmutableMap<FieldDescriptor, Optional<FieldMask<?>>> getSubFieldMasks();

  @Override
  boolean doContains(FieldPath<?> path) {
    if (path.getPath().size() == 1) {
      return getSubFieldMasks().containsKey(Iterables.getOnlyElement(path.getPath()));
    }

    int parentIndex = path.getPath().size() - 1;
    FieldPath<?> parentPath = path.getParentPath(parentIndex);
    FieldPath<?> childPath = path.subPath(parentIndex);

    FieldMask<?> parentMask = doGetSubFieldMask(parentPath);
    return parentMask.contains(childPath);
  }

  @Override
  FieldMask<?> doGetSubFieldMask(FieldPath<?> path) {
    FieldDescriptor first = path.getPath().get(0);

    return getSubFieldMasks().get(first)
        .map(subFieldMask -> {
          if (path.getPath().size() == 1) {
            return subFieldMask;
          }

          return subFieldMask.getSubFieldMask(path.subPath(1));
        }).orElseGet(() -> new AllowAllFieldMask<>(path.getLastField().getMessageType()));
  }

  @Override
  public Optional<com.google.protobuf.FieldMask> toProto() {
    throw new UnsupportedOperationException("TODO");
  }

  @AutoValue.Builder
  abstract static class Builder<M extends Message> extends FieldMask.Builder<M> {
    abstract Builder<M> setDescriptorForType(Descriptor descriptorForType);

    abstract Descriptor getDescriptorForType();

    abstract ImmutableMap.Builder<FieldDescriptor, Optional<FieldMask<?>>> subFieldMasksBuilder();

    @Override
    public Builder<M> addField(FieldDescriptor field) {
      Preconditions.checkArgument(field.getContainingType().equals(getDescriptorForType()));

      subFieldMasksBuilder().put(field, Optional.empty());
      return this;
    }

    @Override
    public Builder<M> addField(FieldDescriptor field, FieldMask<?> subFieldMask) {
      Preconditions.checkArgument(field.getContainingType().equals(getDescriptorForType()));
      Preconditions.checkArgument(field.getJavaType() == JavaType.MESSAGE);
      Preconditions.checkArgument(
          field.getMessageType().equals(subFieldMask.getDescriptorForType()));

      subFieldMasksBuilder().put(field, Optional.of(subFieldMask));
      return this;
    }

    @Override
    public abstract AllowSomeFieldMask<M> build();
  }
}
