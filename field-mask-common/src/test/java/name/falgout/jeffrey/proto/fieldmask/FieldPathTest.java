package name.falgout.jeffrey.proto.fieldmask;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import name.falgout.jeffrey.proto.fieldmask.Test.Bar;
import name.falgout.jeffrey.proto.fieldmask.Test.Foo;
import org.junit.jupiter.api.Test;

class FieldPathTest {
  @Test
  void createFromPathString() {
    FieldPath<Foo> fieldPath = FieldPath.create(Foo.getDefaultInstance(), "bar_field.string_field");

    assertThat(fieldPath.getDescriptorForType()).isEqualTo(Foo.getDescriptor());
    assertThat(fieldPath.getPath()).hasSize(2);
    assertThat(fieldPath.toPathString()).isEqualTo("bar_field.string_field");
  }

  @Test
  void createFromDescriptors() {
    FieldPath<Foo> fieldPath = FieldPath.create(
        Foo.getDefaultInstance(),
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
        () -> FieldPath.create(Foo.getDefaultInstance(), "abc"));
  }

  @Test
  void createFromPathString_intermediateFieldsMustBeMessages() {
    assertThrows(
        IllegalArgumentException.class,
        () -> FieldPath.create(Foo.getDefaultInstance(), "int_field.string_field"));
  }

  @Test
  void createFromDescriptors_fieldsMustNestCorrectly() {
    assertThrows(
        IllegalArgumentException.class,
        () -> FieldPath.create(
            Foo.getDefaultInstance(),
            Bar.getDescriptor().findFieldByNumber(Bar.STRING_FIELD_FIELD_NUMBER)));
  }

  @Test
  void createFromDescriptors_intermediateFieldsMustBeMessages() {
    assertThrows(
        IllegalArgumentException.class,
        () -> FieldPath.create(
            Foo.getDefaultInstance(),
            Foo.getDescriptor().findFieldByNumber(Foo.INT_FIELD_FIELD_NUMBER),
            Bar.getDescriptor().findFieldByNumber(Bar.STRING_FIELD_FIELD_NUMBER)));
  }

  @Test
  void createFromPathString_cannotBeEmpty() {
    assertThrows(
        IllegalArgumentException.class,
        () -> FieldPath.create(Foo.getDefaultInstance(), ""));
  }
}
