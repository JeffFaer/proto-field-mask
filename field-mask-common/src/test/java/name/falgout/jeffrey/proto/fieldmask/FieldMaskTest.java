package name.falgout.jeffrey.proto.fieldmask;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import name.falgout.jeffrey.proto.fieldmask.Test.Bar;
import name.falgout.jeffrey.proto.fieldmask.Test.Foo;
import org.junit.jupiter.api.Test;

class FieldMaskTest {
  @Test
  void allowAll() {
    FieldMask<Foo> allowAll = FieldMask.allowAll(Foo.getDefaultInstance());
    FieldPath<Foo> barPath = FieldPath.create(Foo.getDefaultInstance(), "bar_field");

    assertThat(allowAll.toProto()).isEmpty();
    assertThat(allowAll.contains(FieldPath.create(Foo.getDefaultInstance(), "int_field")))
        .isTrue();
    assertThat(allowAll.contains(barPath)).isTrue();
    assertThat(
        allowAll.contains(FieldPath.create(Foo.getDefaultInstance(), "bar_field.string_field")))
        .isTrue();

    FieldMask<?> subFieldMask = allowAll.getSubFieldMask(barPath);

    assertThat(subFieldMask.toProto()).isEmpty();
    assertThat(subFieldMask.contains(FieldPath.create(Bar.getDefaultInstance(), "string_field")))
        .isTrue();
  }

  @Test
  void contains_checksDescriptorType() {
    FieldMask<Foo> mask = FieldMask.allowAll(Foo.getDefaultInstance());
    FieldPath<Bar> path = FieldPath.create(Bar.getDefaultInstance(), "string_field");

    assertThrows(IllegalArgumentException.class, () -> mask.contains(path));
  }

  @Test
  void getSubFieldMask_checksDescriptorType() {
    FieldMask<Foo> mask = FieldMask.allowAll(Foo.getDefaultInstance());
    FieldPath<Bar> path = FieldPath.create(Bar.getDefaultInstance(), "string_field");

    assertThrows(IllegalArgumentException.class, () -> mask.getSubFieldMask(path));
  }

  @Test
  void getSubFieldMask_checksTerminalFieldType() {
    FieldMask<Foo> mask = FieldMask.allowAll(Foo.getDefaultInstance());
    FieldPath<Foo> path = FieldPath.create(Foo.getDefaultInstance(), "bar_field.string_field");

    assertThrows(IllegalArgumentException.class, () -> mask.getSubFieldMask(path));
  }
}
