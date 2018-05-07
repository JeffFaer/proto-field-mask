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
  private static final Foo FOO = Foo.getDefaultInstance();
  private static final Bar BAR = Bar.getDefaultInstance();
  private static final Baz BAZ = Baz.getDefaultInstance();

  @Nested
  static class AllowAll {
    @Test
    void hasNoProto() {
      assertThat(FieldMask.allowAll(FOO).toProto()).isEmpty();
    }

    @Test
    void containsAll() {
      FieldMask<Foo> allowFoo = FieldMask.allowAll(FOO);

      assertThat(allowFoo.contains(FieldPath.create(FOO))).isTrue();

      for (FieldDescriptor field : Foo.getDescriptor().getFields()) {
        assertThat(allowFoo.contains(FieldPath.create(FOO, field))).isTrue();
      }
    }

    @Test
    void isRecursive() {
      FieldMask<Foo> allowFoo = FieldMask.allowAll(FOO);
      FieldMask<Bar> allowBar = FieldMask.allowAll(BAR);

      assertThat(allowFoo.getSubFieldMask(FieldPath.create(FOO, "bar_field"))).isEqualTo(allowBar);
    }
  }

  @Nested
  static class AllowNone {
    @Test
    void hasDefaultProto() {
      assertThat(FieldMask.allowNone(FOO).toProto().get()).isEqualToDefaultInstance();
    }

    @Test
    void containsNone() {
      FieldMask<Foo> noneOfFoo = FieldMask.allowNone(FOO);

      assertThat(noneOfFoo.contains(FieldPath.create(FOO))).isFalse();

      for (FieldDescriptor field : Foo.getDescriptor().getFields()) {
        assertThat(noneOfFoo.contains(FieldPath.create(FOO, field))).isFalse();
      }
    }

    @Test
    void isRecursive() {
      FieldMask<Foo> noneOfFoo = FieldMask.allowNone(FOO);
      FieldMask<Bar> noneOfBar = FieldMask.allowNone(BAR);

      assertThat(noneOfFoo.getSubFieldMask(FieldPath.create(FOO, "bar_field")))
          .isEqualTo(noneOfBar);
    }
  }

  @Nested
  static class Builder {
    @Test
    void simple() {
      FieldMask<Bar> barMask =
          FieldMask.newBuilder(BAR)
              .addFieldPath(FieldPath.create(BAR, "string_field"))
              .build();

      FieldMask<Foo> fooMask =
          FieldMask.newBuilder(FOO)
              .addFieldPath(FieldPath.create(FOO, "int_field"))
              .addFieldPath(FieldPath.create(FOO, "bar_field.string_field"))
              .build();

      FieldPath<Foo> intField = FieldPath.create(FOO, "int_field");
      FieldPath<Foo> barField = FieldPath.create(FOO, "bar_field");
      FieldPath<Foo> stringField = FieldPath.create(FOO, "bar_field.string_field");
      FieldPath<Foo> bytesField = FieldPath.create(FOO, "bar_field.bytes_field");

      assertThat(fooMask.contains(intField)).isTrue();
      assertThat(fooMask.contains(barField)).isTrue();
      assertThat(fooMask.contains(stringField)).isTrue();
      assertThat(fooMask.contains(bytesField)).isFalse();

      assertThat(fooMask.getSubFieldMask(barField)).isEqualTo(barMask);
    }

    @Test
    void ignoresDuplicates() {
      FieldMask<Foo> mask =
          FieldMask.newBuilder(FOO)
              .addFieldPath(FieldPath.create(FOO, "int_field"))
              .addFieldPath(FieldPath.create(FOO, "int_field"))
              .build();

      assertThat(mask.toProto().get())
          .isEqualTo(com.google.protobuf.FieldMask.newBuilder().addPaths("int_field").build());
    }

    @Test
    void hasProto() {
      FieldMask<Foo> mask =
          FieldMask.newBuilder(FOO)
              .addFieldPath(FieldPath.create(FOO, "int_field"))
              .addFieldPath(FieldPath.create(FOO, "bar_field.string_field"))
              .addFieldPath(FieldPath.create(FOO, "bar_field.bytes_field"))
              .build();

      com.google.protobuf.FieldMask expected =
          com.google.protobuf.FieldMask.newBuilder()
              .addPaths("bar_field.bytes_field")
              .addPaths("bar_field.string_field")
              .addPaths("int_field")
              .build();

      assertThat(mask.toProto().get()).isEqualTo(expected);
    }

    @Test
    void collapsesFields() {
      FieldPath<Foo> barField = FieldPath.create(FOO, "bar_field");
      FieldPath<Foo> stringField = FieldPath.create(FOO, "bar_field.string_field");
      FieldPath<Foo> bytesField = FieldPath.create(FOO, "bar_field.bytes_field");

      FieldMask.Builder<Foo> builder = FieldMask.newBuilder(FOO);

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
    void subFieldMask_deeplyNested() {
      FieldMask<Baz> bazMask = FieldMask.allowAll(BAZ);
      FieldMask<Bar> barMask =
          FieldMask.newBuilder(BAR)
              .addFieldPath(FieldPath.create(BAR, "nested_baz"))
              .build();

      FieldMask<Foo> fooMask =
          FieldMask.newBuilder(FOO)
              .addFieldPath(FieldPath.create(FOO, "bar_field.nested_baz"))
              .build();

      assertThat(fooMask.getSubFieldMask(FieldPath.create(FOO, "bar_field"))).isEqualTo(barMask);
      assertThat(fooMask.getSubFieldMask(FieldPath.create(FOO, "bar_field.nested_baz")))
          .isEqualTo(bazMask);
    }

    @Test
    void ignoresRedundantFields() {
      FieldMask.Builder<Foo> builder = FieldMask.newBuilder(FOO);
      builder.addFieldPath(FieldPath.create(FOO, "bar_field"));

      FieldMask<Foo> first = builder.build();

      builder.addFieldPath(FieldPath.create(FOO, "bar_field.nested_baz"));
      FieldMask<Foo> second = builder.build();

      assertThat(first).isEqualTo(second);
    }
  }

  @Test
  void getSubFieldMask_emptyPath() {
    FieldMask<Foo> mask =
        FieldMask.newBuilder(FOO)
            .addFieldPath(FieldPath.create(FOO, "bar_field"))
            .build();

    assertThat(mask.getSubFieldMask(FieldPath.create(FOO))).isSameAs(mask);
  }

  @Test
  void fromProto() {
    com.google.protobuf.FieldMask proto =
        com.google.protobuf.FieldMask.newBuilder()
            .addPaths("int_field")
            .addPaths("bar_field.nested_baz")
            .addPaths("bar_field.string_field")
            .build();

    FieldMask<Foo> fieldMask = FieldMask.fromProto(FOO, proto);

    assertThat(fieldMask.toProto().get()).isNotEqualTo(proto);
    assertThat(fieldMask.toProto().get()).ignoringRepeatedFieldOrder().isEqualTo(proto);
    assertThat(fieldMask.toProto().get()).isEqualTo(FieldMaskUtil.normalize(proto));
  }

  @Test
  void contains_checksDescriptorType() {
    FieldMask<Foo> mask = FieldMask.allowAll(FOO);
    FieldPath<Bar> path = FieldPath.create(BAR, "string_field");

    assertThrows(IllegalArgumentException.class, () -> mask.contains(path));
  }

  @Test
  void getSubFieldMask_checksDescriptorType() {
    FieldMask<Foo> mask = FieldMask.allowAll(FOO);
    FieldPath<Bar> path = FieldPath.create(BAR, "string_field");

    assertThrows(IllegalArgumentException.class, () -> mask.getSubFieldMask(path));
  }

  @Test
  void getSubFieldMask_checksTerminalFieldType() {
    FieldMask<Foo> mask = FieldMask.allowAll(FOO);
    FieldPath<Foo> path = FieldPath.create(FOO, "bar_field.string_field");

    assertThrows(IllegalArgumentException.class, () -> mask.getSubFieldMask(path));
  }
}
