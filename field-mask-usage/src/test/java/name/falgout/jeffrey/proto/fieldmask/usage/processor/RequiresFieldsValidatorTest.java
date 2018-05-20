package name.falgout.jeffrey.proto.fieldmask.usage.processor;

import com.google.protobuf.Message;
import name.falgout.jeffrey.proto.fieldmask.usage.RequiresFields;
import name.falgout.jeffrey.proto.fieldmask.usage.Test.Root;
import name.falgout.jeffrey.testing.processor.ExpectError;
import name.falgout.jeffrey.testing.processor.UseProcessor;

@UseProcessor(RequiresFieldsValidator.class)
class RequiresFieldsValidatorTest {
  class ValidFieldMasks {
    void test1(@RequiresFields("first_child") Root root) {}

    void test2(@RequiresFields({"first_child", "second_child"}) Root root) {}

    void test3(@RequiresFields({"first_child.value", "second_child"}) Root root) {}
  }

  class InvalidFieldMasks {
    @ExpectError(
        value = "empty",
        lineOffset = 4,
        testName = "@RequiresFields cannot be empty.")
    void test1(@RequiresFields({}) Root root) {}

    @ExpectError(
        value = "com.google.protobuf.Message",
        lineOffset = 4,
        testName = "@RequiresFields must be applied to Messages")
    void test2(@RequiresFields("") Object object) {}

    @ExpectError(
        value = "Invalid field path \"invalid_field\"",
        lineOffset = 4,
        testName = "@RequiresFields field paths must be valid")
    void test3(@RequiresFields("invalid_field") Root root) {}

    @ExpectError(
        value = "Failed to get default instance",
        lineOffset = 4,
        testName = "@RequiresFields must be on subclass of Message")
    void test4(@RequiresFields("field") Message message) {}
  }
}
