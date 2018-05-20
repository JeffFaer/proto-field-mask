package name.falgout.jeffrey.proto.fieldmask.usage.processor;

import com.google.common.base.CaseFormat;
import com.google.errorprone.CompilationTestHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

class FieldUsageValidatorTest {
  private final CompilationTestHelper compiler =
      CompilationTestHelper.newInstance(FieldUsageValidator.class, getClass());

  @Test
  void validUse(TestInfo testInfo) {
    runTest(testInfo);
  }

  @Test
  void invalidUse(TestInfo testInfo) {
    runTest(testInfo);
  }

  private void runTest(TestInfo test) {
    String fileName =
        CaseFormat.LOWER_CAMEL.to(
            CaseFormat.UPPER_CAMEL,
            test.getTestMethod().get().getName()) + ".java";
    compiler.addSourceFile(getPath(fileName)).doTest();
  }

  private static String getPath(String fileName) {
    return "/name/falgout/jeffrey/proto/fieldmask/usage/processor/" + fileName;
  }
}
