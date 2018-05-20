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
    runTest(testInfo, "RootUtils.java");
  }

  @Test
  void invalidUse(TestInfo testInfo) {
    runTest(testInfo, "RootUtils.java");
  }

  private void runTest(TestInfo test, String... extraSourceFiles) {
    for (String sourceFile : extraSourceFiles) {
      compiler.addSourceFile(getPath(sourceFile));
    }

    String fileName =
        CaseFormat.LOWER_CAMEL.to(
            CaseFormat.UPPER_CAMEL,
            test.getTestMethod().get().getName()) + ".java";
    compiler.addSourceFile(getPath(fileName)).matchAllDiagnostics().doTest();
  }

  private static String getPath(String fileName) {
    return "/name/falgout/jeffrey/proto/fieldmask/usage/processor/" + fileName;
  }
}
