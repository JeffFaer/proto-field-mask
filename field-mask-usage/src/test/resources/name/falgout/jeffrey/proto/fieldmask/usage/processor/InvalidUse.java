package name.falgout.jeffrey.proto.fieldmask.usage.processor;

import name.falgout.jeffrey.proto.fieldmask.usage.RequiresFields;
import name.falgout.jeffrey.proto.fieldmask.usage.Test.Root;

class InvalidUse {
  void processRoot(@RequiresFields({"first_child.value"}) Root root) {
    // BUG: Diagnostic contains: second_child
    System.out.println(root.getSecondChild());
  }
}
