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
  private static final Class<Foo> FOO = Foo.class;
  private static final Class<Bar> BAR = Bar.class;
  private static final Class<Baz> BAZ = Baz.class;

  @Nested
  static class AllowAll {
    final FieldMask<Foo> allowAll = FieldMask.allowAll(FOO);

    @Test
    void hasNoProto() {
      assertThat(allowAll.toProto()).isEmpty();
    }

    @Test
    void containsAll() {
      assertThat(allowAll.contains(FieldPath.create(FOO))).isTrue();

      for (FieldDescriptor field : Foo.getDescriptor().getFields()) {
        assertThat(allowAll.contains(FieldPath.create(FOO, field))).isTrue();
      }
    }

    @Test
    void isRecursive() {
      FieldMask<Bar> allOfBar = FieldMask.allowAll(BAR);

      assertThat(allowAll.getSubFieldMask(FieldPath.create(FOO, "bar_field"))).isEqualTo(allOfBar);
    }
  }

  @Nested
  static class AllowNone {
    final FieldMask<Foo> allowNone = FieldMask.allowNone(FOO);

    @Test
    void hasDefaultProto() {
      assertThat(allowNone.toProto().get()).isEqualToDefaultInstance();
    }

    @Test
    void containsNone() {
      assertThat(allowNone.contains(FieldPath.create(FOO))).isFalse();

      for (FieldDescriptor field : Foo.getDescriptor().getFields()) {
        assertThat(allowNone.contains(FieldPath.create(FOO, field))).isFalse();
      }
    }

    @Test
    void isRecursive() {
      FieldMask<Bar> noneOfBar = FieldMask.allowNone(BAR);

      assertThat(allowNone.getSubFieldMask(FieldPath.create(FOO, "bar_field")))
          .isEqualTo(noneOfBar);
    }
  }

  @Nested
  static class Builder {
    @Test
    void ignoresDuplicates() {
      FieldPath<Foo> path = FieldPath.create(FOO, "int_field");

      FieldMask<Foo> maskWithDuplicate =
          FieldMask.newBuilder(FOO).addFieldPath(path).addFieldPath(path).build();
      FieldMask<Foo> maskWithoutDuplicate = FieldMask.newBuilder(FOO).addFieldPath(path).build();

      assertThat(maskWithDuplicate).isEqualTo(maskWithoutDuplicate);
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
  void contains() {
    FieldMask<Foo> fooMask =
        FieldMask.of(
            FieldPath.create(FOO, "int_field"),
            FieldPath.create(FOO, "bar_field.string_field"));

    FieldPath<Foo> intField = FieldPath.create(FOO, "int_field");
    FieldPath<Foo> barField = FieldPath.create(FOO, "bar_field");
    FieldPath<Foo> bazField = FieldPath.create(FOO, "baz_field");
    FieldPath<Foo> stringField = FieldPath.create(FOO, "bar_field.string_field");
    FieldPath<Foo> bytesField = FieldPath.create(FOO, "bar_field.bytes_field");

    assertThat(fooMask.contains(intField)).isTrue();
    assertThat(fooMask.contains(barField)).isTrue();
    assertThat(fooMask.contains(bazField)).isFalse();
    assertThat(fooMask.contains(stringField)).isTrue();
    assertThat(fooMask.contains(bytesField)).isFalse();
  }

  @Test
  void containsAll() {
    FieldMask<Foo> haystack =
        FieldMask.of(
            FieldPath.create(FOO, "int_field"),
            FieldPath.create(FOO, "bar_field"));
    FieldMask<Foo> needle1 = FieldMask.of(FieldPath.create(FOO, "int_field"));
    FieldMask<Foo> needle2 = FieldMask.of(FieldPath.create(FOO, "bar_field.string_field"));
    FieldMask<Foo> needle3 =
        FieldMask.of(
            FieldPath.create(FOO, "int_field"),
            FieldPath.create(FOO, "bar_field"));
    FieldMask<Foo> needle4 = FieldMask.of(FieldPath.create(FOO, "baz_field"));

    FieldMask<Foo> allowAll = FieldMask.allowAll(FOO);
    FieldMask<Foo> allowNone = FieldMask.allowNone(FOO);

    assertThat(haystack.containsAll(needle1)).isTrue();
    assertThat(haystack.containsAll(needle2)).isTrue();
    assertThat(haystack.containsAll(needle3)).isTrue();
    assertThat(haystack.containsAll(needle4)).isFalse();
    assertThat(haystack.containsAll(allowAll)).isFalse();
    assertThat(haystack.containsAll(allowNone)).isTrue();

    assertThat(allowAll.containsAll(haystack)).isTrue();
    assertThat(allowAll.containsAll(needle1)).isTrue();
    assertThat(allowAll.containsAll(needle2)).isTrue();
    assertThat(allowAll.containsAll(needle3)).isTrue();
    assertThat(allowAll.containsAll(needle4)).isTrue();
    assertThat(allowAll.containsAll(allowAll)).isTrue();
    assertThat(allowAll.containsAll(allowNone)).isTrue();

    assertThat(allowNone.containsAll(haystack)).isFalse();
    assertThat(allowNone.containsAll(needle1)).isFalse();
    assertThat(allowNone.containsAll(needle2)).isFalse();
    assertThat(allowNone.containsAll(needle3)).isFalse();
    assertThat(allowNone.containsAll(needle4)).isFalse();
    assertThat(allowNone.containsAll(allowAll)).isFalse();
    assertThat(allowNone.containsAll(allowNone)).isTrue();
  }

  @Test
  void getSubFieldMask() {
    FieldMask<Foo> mask =
        FieldMask.of(
            FieldPath.create(FOO, "bar_field.nested_baz"),
            FieldPath.create(FOO, "bar_field.string_field"),
            FieldPath.create(FOO, "int_field"));

    assertThat(mask.getSubFieldMask(FieldPath.create(FOO, "baz_field")))
        .isEqualTo(FieldMask.allowNone(BAZ));
    assertThat(mask.getSubFieldMask(FieldPath.create(FOO, "bar_field")))
        .isEqualTo(
            FieldMask.of(
                FieldPath.create(BAR, "nested_baz"),
                FieldPath.create(BAR, "string_field")));
    assertThat(mask.getSubFieldMask(FieldPath.create(FOO, "bar_field.nested_baz")))
        .isEqualTo(FieldMask.allowAll(BAZ));
  }

  @Test
  void getSubFieldMask_emptyPath() {
    FieldMask<Foo> mask = FieldMask.of(FieldPath.create(FOO, "bar_field"));

    assertThat(mask.getSubFieldMask(FieldPath.create(FOO))).isSameAs(mask);
  }

  @Test
  void getSubFieldMask_checksTerminalFieldType() {
    FieldMask<Foo> mask = FieldMask.allowAll(FOO);
    FieldPath<Foo> path = FieldPath.create(FOO, "bar_field.string_field");

    assertThrows(IllegalArgumentException.class, () -> mask.getSubFieldMask(path));
  }

  @Test
  void toProto_isNormalized() {
    FieldMask<Foo> mask =
        FieldMask.of(
            FieldPath.create(FOO, "int_field"),
            FieldPath.create(FOO, "bar_field.string_field"),
            FieldPath.create(FOO, "bar_field.bytes_field"));

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

    FieldMask<Foo> fieldMask = FieldMask.fromProto(FOO, proto);

    assertThat(fieldMask.toProto().get()).ignoringRepeatedFieldOrder().isEqualTo(proto);
  }

  @Test
  void fromProto_empty() {
    com.google.protobuf.FieldMask proto = com.google.protobuf.FieldMask.getDefaultInstance();
    FieldMask<Foo> fieldMask = FieldMask.fromProto(FOO, proto);

    assertThat(fieldMask).isEqualTo(FieldMask.allowNone(FOO));
  }
}
