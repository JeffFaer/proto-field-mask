package name.falgout.jeffrey.proto.fieldmask;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import name.falgout.jeffrey.proto.fieldmask.Test.Bar;
import name.falgout.jeffrey.proto.fieldmask.Test.Foo;
import org.junit.jupiter.api.Test;

class FieldPathTest {
  @Test
  void createFromPathString() {
    FieldPath<Foo> fieldPath = FieldPath.create(Foo.class, "bar_field.string_field");

    assertThat(fieldPath.getDescriptorForType()).isEqualTo(Foo.getDescriptor());
    assertThat(fieldPath.getPath()).hasSize(2);
    assertThat(fieldPath.toPathString()).isEqualTo("bar_field.string_field");
  }

  @Test
  void createFromDescriptors() {
    FieldPath<Foo> fieldPath = FieldPath.create(
        Foo.class,
        Foo.getDescriptor().findFieldByName("bar_field"),
        Bar.getDescriptor().findFieldByNumber(Bar.STRING_FIELD_FIELD_NUMBER));

    assertThat(fieldPath.getDescriptorForType()).isEqualTo(Foo.getDescriptor());
    assertThat(fieldPath.getPath()).hasSize(2);
    assertThat(fieldPath.toPathString()).isEqualTo("bar_field.string_field");
  }

  @Test
  void createFromPathString_fieldsMustExist() {
    assertThrows(
        IllegalArgumentException.class,
        () -> FieldPath.create(Foo.class, "abc"));
  }

  @Test
  void createFromPathString_intermediateFieldsMustBeMessages() {
    assertThrows(
        IllegalArgumentException.class,
        () -> FieldPath.create(Foo.class, "int_field.string_field"));
  }

  @Test
  void createFromDescriptors_fieldsMustNestCorrectly() {
    assertThrows(
        IllegalArgumentException.class,
        () -> FieldPath.create(
            Foo.class, Bar.getDescriptor().findFieldByNumber(Bar.STRING_FIELD_FIELD_NUMBER)));
  }

  @Test
  void createFromDescriptors_intermediateFieldsMustBeMessages() {
    assertThrows(
        IllegalArgumentException.class,
        () -> FieldPath.create(
            Foo.class,
            Foo.getDescriptor().findFieldByNumber(Foo.INT_FIELD_FIELD_NUMBER),
            Bar.getDescriptor().findFieldByNumber(Bar.STRING_FIELD_FIELD_NUMBER)));
  }

  @Test
  void createFromPathString_cannotBeEmpty() {
    assertThrows(IllegalArgumentException.class, () -> FieldPath.create(Foo.class, ""));
  }

  @Test
  void append() {
    FieldPath<Foo> path = FieldPath.create(Foo.class, "bar_field");
    FieldPath<Foo> fullPath = FieldPath.create(Foo.class, "bar_field.string_field");

    FieldPath<Foo> appended =
        FieldPath.append(path, Bar.getDescriptor().findFieldByName("string_field"));

    assertThat(appended).isEqualTo(fullPath);
  }

  @Test
  void append_toEmptyPath() {
    FieldPath<Foo> path = FieldPath.create(Foo.class);
    FieldPath<Foo> fullPath = FieldPath.create(Foo.class, "bar_field");

    FieldPath<Foo> appended =
        FieldPath.append(path, Foo.getDescriptor().findFieldByName("bar_field"));

    assertThat(appended).isEqualTo(fullPath);
  }
}
