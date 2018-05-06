package name.falgout.jeffrey.proto.fieldmask;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import name.falgout.jeffrey.proto.fieldmask.Test.Bar;
import name.falgout.jeffrey.proto.fieldmask.Test.Baz;
import name.falgout.jeffrey.proto.fieldmask.Test.Foo;
import org.junit.jupiter.api.Test;

class FieldMaskTest {
  private static final Foo FOO = Foo.getDefaultInstance();
  private static final Bar BAR = Bar.getDefaultInstance();
  private static final Baz BAZ = Baz.getDefaultInstance();

  @Test
  void allowAll() {
    FieldMask<Foo> allowAll = FieldMask.allowAll(FOO);
    FieldPath<Foo> barPath = FieldPath.create(FOO, "bar_field");

    assertThat(allowAll.toProto()).isEmpty();
    assertThat(allowAll.contains(FieldPath.create(FOO, "int_field"))).isTrue();
    assertThat(allowAll.contains(barPath)).isTrue();
    assertThat(allowAll.contains(FieldPath.create(FOO, "bar_field.string_field")))
        .isTrue();

    FieldMask<?> subFieldMask = allowAll.getSubFieldMask(barPath);

    assertThat(subFieldMask.toProto()).isEmpty();
    assertThat(subFieldMask.contains(FieldPath.create(BAR, "string_field"))).isTrue();
  }

  @Test
  void allowSome() {
    FieldMask.Builder<Bar> barMaskBuilder = FieldMask.newBuilder(BAR);
    barMaskBuilder.addField(Bar.getDescriptor().findFieldByNumber(Bar.STRING_FIELD_FIELD_NUMBER));

    FieldMask<Bar> barMask = barMaskBuilder.build();

    FieldMask.Builder<Foo> fooMaskBuilder = FieldMask.newBuilder(FOO);
    fooMaskBuilder.addField(Foo.getDescriptor().findFieldByNumber(Foo.INT_FIELD_FIELD_NUMBER));
    fooMaskBuilder.addField(
        Foo.getDescriptor().findFieldByNumber(Foo.BAR_FIELD_FIELD_NUMBER),
        barMask);

    FieldMask<Foo> fooMask = fooMaskBuilder.build();

    FieldPath<Foo> intField = FieldPath.create(FOO, "int_field");
    FieldPath<Foo> barField = FieldPath.create(FOO, "bar_field");
    FieldPath<Foo> stringField = FieldPath.create(FOO, "bar_field.string_field");
    FieldPath<Foo> bytesField = FieldPath.create(FOO, "bar_field.bytes_field");

    assertThat(fooMask.contains(intField)).isTrue();
    assertThat(fooMask.contains(barField)).isTrue();
    assertThat(fooMask.contains(stringField)).isTrue();
    assertThat(fooMask.getSubFieldMask(barField)).isEqualTo(barMask);

    assertThat(fooMask.contains(bytesField)).isFalse();
  }

  @Test
  void allowSome_deeplyNestedSubField() {
    FieldMask<Baz> bazMask = FieldMask.allowAll(BAZ);
    FieldMask<Bar> barMask = FieldMask.newBuilder(BAR)
        .addField(Bar.getDescriptor().findFieldByNumber(Bar.NESTED_BAZ_FIELD_NUMBER), bazMask)
        .build();
    FieldMask<Foo> fooMask = FieldMask.newBuilder(FOO)
        .addField(Foo.getDescriptor().findFieldByNumber(Foo.BAR_FIELD_FIELD_NUMBER), barMask)
        .build();

    assertThat(fooMask.getSubFieldMask(FieldPath.create(FOO, "bar_field"))).isEqualTo(barMask);
    assertThat(fooMask.getSubFieldMask(FieldPath.create(FOO, "bar_field.nested_baz")))
        .isEqualTo(bazMask);
  }

  @Test
  void builder_defaultsToAllowAll() {
    FieldMask.Builder<Foo> builder = FieldMask.newBuilder(FOO);
    builder.addField(Foo.getDescriptor().findFieldByNumber(Foo.BAR_FIELD_FIELD_NUMBER));

    FieldMask<Foo> mask = builder.build();
    assertThat(mask.contains(FieldPath.create(FOO, "bar_field"))).isTrue();
    assertThat(mask.contains(FieldPath.create(FOO, "bar_field.string_field"))).isTrue();
    assertThat(mask.contains(FieldPath.create(FOO, "bar_field.bytes_field"))).isTrue();
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

  @Test
  void builder_addField_checksDescriptorType() {
    FieldMask.Builder<Foo> builder = FieldMask.newBuilder(FOO);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            builder.addField(Bar.getDescriptor().findFieldByNumber(Bar.STRING_FIELD_FIELD_NUMBER)));
  }

  @Test
  void builder_addField_withMask_checksDescriptorType() {
    FieldMask<Bar> barMask = FieldMask.allowAll(BAR);

    FieldMask.Builder<Foo> builder = FieldMask.newBuilder(FOO);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            builder.addField(
                Bar.getDescriptor().findFieldByNumber(Bar.STRING_FIELD_FIELD_NUMBER),
                barMask));
  }

  @Test
  void builder_addField_withMask_checksFieldType() {
    FieldMask<Bar> barMask = FieldMask.allowAll(BAR);

    FieldMask.Builder<Foo> builder = FieldMask.newBuilder(FOO);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            builder.addField(
                Foo.getDescriptor().findFieldByNumber(Foo.INT_FIELD_FIELD_NUMBER),
                barMask));
  }

  @Test
  void builder_addField_withMask_checksFieldMaskType() {
    FieldMask<Bar> barMask = FieldMask.allowAll(BAR);

    FieldMask.Builder<Foo> builder = FieldMask.newBuilder(FOO);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            builder.addField(
                Foo.getDescriptor().findFieldByNumber(Foo.BAZ_FIELD_FIELD_NUMBER),
                barMask));
  }
}
