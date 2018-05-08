package name.falgout.jeffrey.proto.fieldmask;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static com.google.common.truth.extensions.proto.ProtoTruth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.util.FieldMaskUtil;
import name.falgout.jeffrey.proto.fieldmask.Test.Bar;
import name.falgout.jeffrey.proto.fieldmask.Test.Baz;
import name.falgout.jeffrey.proto.fieldmask.Test.Foo;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FieldMaskTest {
  @Nested
  static class AllowAll {
    final FieldMask<Foo> allowAll = FieldMask.allowAll(Foo.class);

    @Test
    void hasNoProto() {
      assertThat(allowAll.toProto()).isEmpty();
    }

    @Test
    void containsAll() {
      assertThat(allowAll.contains(FieldPath.create(Foo.class))).isTrue();

      for (FieldDescriptor field : Foo.getDescriptor().getFields()) {
        assertThat(allowAll.contains(FieldPath.create(Foo.class, field))).isTrue();
      }
    }

    @Test
    void isRecursive() {
      FieldMask<Bar> allOfBar = FieldMask.allowAll(Bar.class);

      assertThat(allowAll.getSubFieldMask(FieldPath.create(Foo.class, "bar_field")))
          .isEqualTo(allOfBar);
    }
  }

  @Nested
  static class AllowNone {
    final FieldMask<Foo> allowNone = FieldMask.allowNone(Foo.class);

    @Test
    void hasDefaultProto() {
      assertThat(allowNone.toProto().get()).isEqualToDefaultInstance();
    }

    @Test
    void containsNone() {
      assertThat(allowNone.contains(FieldPath.create(Foo.class))).isFalse();

      for (FieldDescriptor field : Foo.getDescriptor().getFields()) {
        assertThat(allowNone.contains(FieldPath.create(Foo.class, field))).isFalse();
      }
    }

    @Test
    void isRecursive() {
      FieldMask<Bar> noneOfBar = FieldMask.allowNone(Bar.class);

      assertThat(allowNone.getSubFieldMask(FieldPath.create(Foo.class, "bar_field")))
          .isEqualTo(noneOfBar);
    }
  }

  @Nested
  static class Builder {
    @Test
    void ignoresDuplicates() {
      FieldPath<Foo> path = FieldPath.create(Foo.class, "int_field");

      FieldMask<Foo> maskWithDuplicate =
          FieldMask.newBuilder(Foo.class).addFieldPath(path).addFieldPath(path).build();
      FieldMask<Foo> maskWithoutDuplicate =
          FieldMask.newBuilder(Foo.class).addFieldPath(path).build();

      assertThat(maskWithDuplicate).isEqualTo(maskWithoutDuplicate);
    }

    @Test
    void collapsesFields() {
      FieldPath<Foo> barField = FieldPath.create(Foo.class, "bar_field");
      FieldPath<Foo> stringField = FieldPath.create(Foo.class, "bar_field.string_field");
      FieldPath<Foo> bytesField = FieldPath.create(Foo.class, "bar_field.bytes_field");

      FieldMask.Builder<Foo> builder = FieldMask.newBuilder(Foo.class);

      builder.addFieldPath(stringField);
      FieldMask<Foo> first = builder.build();

      builder.addFieldPath(barField);
      FieldMask<Foo> second = builder.build();

      assertThat(first).isNotEqualTo(second);

      assertThat(first.contains(barField)).isTrue();
      assertThat(first.contains(stringField)).isTrue();
      assertThat(first.contains(bytesField)).isFalse();

      assertThat(second.contains(barField)).isTrue();
      assertThat(second.contains(stringField)).isTrue();
      assertThat(second.contains(bytesField)).isTrue();
    }

    @Test
    void ignoresRedundantFields() {
      FieldMask.Builder<Foo> builder = FieldMask.newBuilder(Foo.class);
      builder.addFieldPath(FieldPath.create(Foo.class, "bar_field"));

      FieldMask<Foo> first = builder.build();

      builder.addFieldPath(FieldPath.create(Foo.class, "bar_field.nested_baz"));
      FieldMask<Foo> second = builder.build();

      assertThat(first).isEqualTo(second);
    }
  }

  @Test
  void contains_checksDescriptorType() {
    FieldMask<Foo> mask = FieldMask.allowAll(Foo.class);
    FieldPath<Bar> path = FieldPath.create(Bar.class, "string_field");

    assertThrows(IllegalArgumentException.class, () -> mask.contains(path));
  }

  @Test
  void contains() {
    FieldMask<Foo> fooMask =
        FieldMask.of(
            FieldPath.create(Foo.class, "int_field"),
            FieldPath.create(Foo.class, "bar_field.string_field"));

    FieldPath<Foo> intField = FieldPath.create(Foo.class, "int_field");
    FieldPath<Foo> barField = FieldPath.create(Foo.class, "bar_field");
    FieldPath<Foo> bazField = FieldPath.create(Foo.class, "baz_field");
    FieldPath<Foo> stringField = FieldPath.create(Foo.class, "bar_field.string_field");
    FieldPath<Foo> bytesField = FieldPath.create(Foo.class, "bar_field.bytes_field");

    assertThat(fooMask.contains(intField)).isTrue();
    assertThat(fooMask.contains(barField)).isTrue();
    assertThat(fooMask.contains(bazField)).isFalse();
    assertThat(fooMask.contains(stringField)).isTrue();
    assertThat(fooMask.contains(bytesField)).isFalse();
  }

  @Test
  void getSubFieldMask() {
    FieldMask<Foo> mask =
        FieldMask.of(
            FieldPath.create(Foo.class, "bar_field.nested_baz"),
            FieldPath.create(Foo.class, "bar_field.string_field"),
            FieldPath.create(Foo.class, "int_field"));

    assertThat(mask.getSubFieldMask(FieldPath.create(Foo.class, "baz_field")))
        .isEqualTo(FieldMask.allowNone(Baz.class));
    assertThat(mask.getSubFieldMask(FieldPath.create(Foo.class, "bar_field")))
        .isEqualTo(
            FieldMask.of(
                FieldPath.create(Bar.class, "nested_baz"),
                FieldPath.create(Bar.class, "string_field")));
    assertThat(mask.getSubFieldMask(FieldPath.create(Foo.class, "bar_field.nested_baz")))
        .isEqualTo(FieldMask.allowAll(Baz.class));
  }

  @Test
  void getSubFieldMask_emptyPath() {
    FieldMask<Foo> mask = FieldMask.of(FieldPath.create(Foo.class, "bar_field"));

    assertThat(mask.getSubFieldMask(FieldPath.create(Foo.class))).isSameAs(mask);
  }

  @Test
  void getSubFieldMask_checksDescriptorType() {
    FieldMask<Foo> mask = FieldMask.allowAll(Foo.class);
    FieldPath<Bar> path = FieldPath.create(Bar.class, "string_field");

    assertThrows(IllegalArgumentException.class, () -> mask.getSubFieldMask(path));
  }

  @Test
  void getSubFieldMask_checksTerminalFieldType() {
    FieldMask<Foo> mask = FieldMask.allowAll(Foo.class);
    FieldPath<Foo> path = FieldPath.create(Foo.class, "bar_field.string_field");

    assertThrows(IllegalArgumentException.class, () -> mask.getSubFieldMask(path));
  }

  @Test
  void toProto_isNormalized() {
    FieldMask<Foo> mask =
        FieldMask.of(
            FieldPath.create(Foo.class, "int_field"),
            FieldPath.create(Foo.class, "bar_field.string_field"),
            FieldPath.create(Foo.class, "bar_field.bytes_field"));

    com.google.protobuf.FieldMask expected =
        com.google.protobuf.FieldMask.newBuilder()
            .addPaths("int_field")
            .addPaths("bar_field.string_field")
            .addPaths("bar_field.bytes_field")
            .build();

    com.google.protobuf.FieldMask actual = mask.toProto().get();

    assertThat(actual).ignoringRepeatedFieldOrder().isEqualTo(expected);

    assertThat(actual).isNotEqualTo(expected);
    assertThat(actual).isEqualTo(FieldMaskUtil.normalize(expected));
  }

  @Test
  void fromProto() {
    com.google.protobuf.FieldMask proto =
        com.google.protobuf.FieldMask.newBuilder()
            .addPaths("int_field")
            .addPaths("bar_field.nested_baz")
            .addPaths("bar_field.string_field")
            .build();

    FieldMask<Foo> fieldMask = FieldMask.fromProto(Foo.class, proto);

    assertThat(fieldMask.toProto().get()).ignoringRepeatedFieldOrder().isEqualTo(proto);
  }

  @Test
  void fromProto_empty() {
    com.google.protobuf.FieldMask proto = com.google.protobuf.FieldMask.getDefaultInstance();
    FieldMask<Foo> fieldMask = FieldMask.fromProto(Foo.class, proto);

    assertThat(fieldMask).isEqualTo(FieldMask.allowNone(Foo.class));
  }
}
