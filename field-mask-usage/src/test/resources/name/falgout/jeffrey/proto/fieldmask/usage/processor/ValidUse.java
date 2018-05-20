package name.falgout.jeffrey.proto.fieldmask.usage.processor;

import name.falgout.jeffrey.proto.fieldmask.usage.RequiresFields;
import name.falgout.jeffrey.proto.fieldmask.usage.Test.Root;

class ValidUse {
  void processRoot(@RequiresFields({"first_child", "second_child.value"}) Root root) {
    System.out.println(root.getFirstChild());
    System.out.println(root.getSecondChild().getValue());
  }
}
