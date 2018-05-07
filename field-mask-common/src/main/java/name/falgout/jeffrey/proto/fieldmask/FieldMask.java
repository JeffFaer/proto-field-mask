package name.falgout.jeffrey.proto.fieldmask;

import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.DoNotMock;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.Message;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@DoNotMock
@Immutable
public final class FieldMask<M extends Message> {
  public static <M extends Message> FieldMask<M> allowAll(M prototype) {
    return newBuilder(prototype).addFieldPath(FieldPath.create(prototype)).build();
  }

  public static <M extends Message> FieldMask<M> allowNone(M prototype) {
    return newBuilder(prototype).build();
  }

  public static <M extends Message> Builder<M> newBuilder(M prototype) {
    return new Builder<>(prototype.getDescriptorForType());
  }

  public static <M extends Message> FieldMask<M> fromProto(
      M prototype, com.google.protobuf.FieldMask fieldMask) {
    Builder<M> builder = new Builder<>(prototype.getDescriptorForType());

    fieldMask.getPathsList()
        .stream()
        .map(pathString -> FieldPath.create(prototype, pathString))
        .forEach(builder::addFieldPath);

    return builder.build();
  }

  private static class Node {
    /**
     * <p>If children == null, then this node contains all sub-fields.
     * <p>If children.isEmpty, then this node contains none of its sub-fields.
     */
    @Nullable SortedMap<FieldDescriptor, Node> children =
        new TreeMap<>(Comparator.comparing(FieldDescriptor::getName));

    Node() {}

    Node(Node other) {
      if (other.children == null) {
        this.children = null;
      } else {
        other.children.forEach((field, node) -> children.put(field, new Node(node)));
      }
    }

    <M extends Message> Stream<FieldPath<M>> listFieldPaths(FieldPath<M> prefix) {
      if (children == null) {
        return Stream.of(prefix);
      }

      return children.entrySet()
          .stream()
          .flatMap(entry -> {
            FieldDescriptor field = entry.getKey();
            FieldPath<M> nextPrefix = FieldPath.append(prefix, field);

            Node subTree = entry.getValue();

            return subTree.listFieldPaths(nextPrefix);
          });
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Node)) {
        return false;
      }
      Node node = (Node) o;
      return Objects.equals(children, node.children);
    }

    @Override
    public int hashCode() {
      return Objects.hash(children);
    }

    @Override
    public String toString() {
      return "Node{" +
          "children=" + children +
          '}';
    }
  }

  private final Descriptor descriptor;
  private final Node root;

  FieldMask(Descriptor descriptor, Node root) {
    this.descriptor = descriptor;
    this.root = root;
  }

  /** The {@link Descriptor} for {@code M} */
  public Descriptor getDescriptorForType() {
    return descriptor;
  }

  /**
   * Determines whether the given {@link FieldPath} is included in this {@code FieldMask}.
   *
   * @throws IllegalArgumentException if the {@code path}'s {@linkplain
   *     FieldPath#getDescriptorForType() type} is not equal to this {@code FieldMask}'s {@linkplain
   *     #getDescriptorForType() type}
   */
  public boolean contains(FieldPath<?> path) {
    Preconditions.checkArgument(path.getDescriptorForType().equals(descriptor));

    Node node = root;
    for (FieldDescriptor field : path.getPath()) {
      if (node.children == null) {
        return true;
      }

      if (!node.children.containsKey(field)) {
        return false;
      }

      node = node.children.get(field);
    }

    return node.children == null || !node.children.isEmpty();
  }

  /**
   * Returns a {@code FieldMask} for the given {@code FieldPath}.
   *
   * @throws IllegalArgumentException if the {@code path}'s {@linkplain
   *     FieldPath#getDescriptorForType() type} is not equal to this {@code FieldMask}'s {@linkplain
   *     #getDescriptorForType() type} or if the {@code path} does not {@linkplain
   *     FieldPath#getLastField() terminate} in a {@linkplain JavaType#MESSAGE message}
   */
  public final FieldMask<?> getSubFieldMask(FieldPath<?> path) {
    Preconditions.checkArgument(path.getDescriptorForType().equals(descriptor));
    Preconditions.checkArgument(path.getLastField().getJavaType() == JavaType.MESSAGE);

    Descriptor subFieldDescriptor = path.getLastField().getMessageType();

    Node node = root;
    for (FieldDescriptor field : path.getPath()) {
      if (node.children == null) {
        return new FieldMask<>(subFieldDescriptor, node);
      }

      if (!node.children.containsKey(field)) {
        return new FieldMask(subFieldDescriptor, new Node());
      }

      node = node.children.get(field);
    }

    return new FieldMask<>(subFieldDescriptor, node);
  }

  /**
   * Converts this to a {@code FieldMask} to a {@linkplain com.google.protobuf.util.FieldMaskUtil#normalize(com.google.protobuf.FieldMask)
   * normalized} {@link com.google.protobuf.FieldMask}.
   */
  public final Optional<com.google.protobuf.FieldMask> toProto() {
    if (root.children == null) {
      return Optional.empty();
    }

    com.google.protobuf.FieldMask.Builder fieldMask = com.google.protobuf.FieldMask.newBuilder();
    root.listFieldPaths(FieldPath.create(descriptor))
        .map(FieldPath::toPathString)
        .forEachOrdered(fieldMask::addPaths);

    return Optional.of(fieldMask.build());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof FieldMask)) {
      return false;
    }
    FieldMask<?> fieldMask = (FieldMask<?>) o;
    return Objects.equals(descriptor, fieldMask.descriptor) &&
        Objects.equals(root, fieldMask.root);
  }

  @Override
  public int hashCode() {
    return Objects.hash(descriptor, root);
  }

  @Override
  public String toString() {
    return "FieldMask{" +
        "descriptor=" + descriptor.getFullName() +
        ", root=" + root +
        '}';
  }

  /** A builder for {@code FieldMask}s. */
  public final static class Builder<M extends Message> {
    private final Descriptor descriptor;
    private Node root = new Node();
    private boolean dirty = false;

    Builder(Descriptor descriptor) {
      this.descriptor = descriptor;
    }

    Node getRoot() {
      if (dirty) {
        dirty = false;
        root = new Node(root);
      }

      return root;
    }

    /**
     * Add the specified {@code path} to this {@code Builder}.
     *
     * If the {@code path} ends in a {@linkplain JavaType#MESSAGE message}, all of its sub-fields
     * will be recursively included.
     *
     * @throws IllegalArgumentException if the {@code field}'s {@linkplain
     *     FieldDescriptor#getMessageType() type} does not match the {@code subFieldMask}'s
     *     {@linkplain FieldMask#getDescriptorForType() type}.
     */
    public Builder<M> addFieldPath(FieldPath<M> path) {
      Node node = getRoot();
      for (FieldDescriptor field : path.getPath()) {
        if (node.children == null) {
          return this;
        }

        node = node.children.computeIfAbsent(field, ignored -> new Node());
      }

      node.children = null;
      return this;
    }

    public FieldMask<M> build() {
      dirty = true;
      return new FieldMask<>(descriptor, root);
    }
  }
}
