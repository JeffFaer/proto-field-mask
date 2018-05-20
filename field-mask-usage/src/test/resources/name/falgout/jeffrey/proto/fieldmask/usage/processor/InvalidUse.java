package name.falgout.jeffrey.proto.fieldmask.usage.processor;

import name.falgout.jeffrey.proto.fieldmask.usage.RequiresFields;
import name.falgout.jeffrey.proto.fieldmask.usage.Test.Child;
import name.falgout.jeffrey.proto.fieldmask.usage.Test.Root;

class InvalidUse {
  void processRoot(@RequiresFields("first_child") Root root) {
    // BUG: Diagnostic contains: second_child
    System.out.println(root.getSecondChild());
  }

  interface DefaultReturnsAreChecked {
    default Child getChild(@RequiresFields("second_child") Root root) {
      // BUG: Diagnostic contains: first_child
      return root.getFirstChild();
    }
  }

  void followsLocalVariables(@RequiresFields("first_child.value") Root root) {
    Child firstChild = root.getFirstChild();
    // BUG: Diagnostic contains: description
    firstChild.getDescription();

    Child firstChildAgain = firstChild;
    // BUG: Diagnostic contains: description
    firstChildAgain.getDescription();
  }

  void followsLocalVariableReassignment(
      @RequiresFields({"first_child.value", "second_child.description"}) Root root) {
    Child child = root.getFirstChild();
    child.getValue();
    // BUG: Diagnostic contains: description
    child.getDescription();

    child = root.getSecondChild();
    // BUG: Diagnostic contains: value
    child.getValue();
    child.getDescription();

    Child secondChild = child;
    // BUG: Diagnostic contains: value
    secondChild.getValue();
  }
}
