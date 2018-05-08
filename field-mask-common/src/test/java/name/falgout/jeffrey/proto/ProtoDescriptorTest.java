package name.falgout.jeffrey.proto;

import static com.google.common.truth.Truth.assertThat;

import name.falgout.jeffrey.proto.fieldmask.Test.Foo;
import org.junit.jupiter.api.Test;

class ProtoDescriptorTest {
  @Test
  void castTo() {
    ProtoDescriptor<Foo> foo = ProtoDescriptor.create(Foo.class);
    ProtoDescriptor<?> unknown = ProtoDescriptor.create(Foo.getDescriptor());

    assertThat(foo).isEqualTo(unknown);
    assertThat(unknown.castTo(foo)).isSameAs(unknown);
  }
}
