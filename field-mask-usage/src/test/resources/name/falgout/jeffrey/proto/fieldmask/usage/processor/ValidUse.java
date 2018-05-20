package name.falgout.jeffrey.proto.fieldmask.usage.processor;

import name.falgout.jeffrey.proto.fieldmask.usage.RequiresFields;
import name.falgout.jeffrey.proto.fieldmask.usage.Test.Child;
import name.falgout.jeffrey.proto.fieldmask.usage.Test.Root;

class ValidUse {
  void processRoot(@RequiresFields({"first_child", "second_child.value"}) Root root) {
    System.out.println(root.getFirstChild());
    System.out.println(root.getSecondChild().getValue());
  }

  void followsLocalVariables(@RequiresFields("first_child.value") Root root) {
    Child firstChild = root.getFirstChild();
    firstChild.getValue();

    Child firstChildAgain = firstChild;
    firstChildAgain.getValue();
  }

  void followsLocalVariableReassignment(
      @RequiresFields({"first_child.value", "second_child.description"}) Root root) {
    Child child = root.getFirstChild();
    child.getValue();

    child = root.getSecondChild();
    child.getDescription();

    Child secondChild = child;
    secondChild.getDescription();
  }

  void multipleAnnotatedParameters(
      @RequiresFields("first_child") Root firstRoot,
      @RequiresFields("second_child") Root secondRoot) {
    firstRoot.getFirstChild();
    secondRoot.getSecondChild();

    Root tmp1 = firstRoot;
    Root tmp2 = secondRoot;

    tmp1.getFirstChild();
    tmp2.getSecondChild();

    Root tmp3 = tmp1;
    tmp1 = tmp2;
    tmp2 = tmp3;

    tmp1.getSecondChild();
    tmp2.getFirstChild();
  }

  void reassigningExplicityAnnotatedVariables(
      @RequiresFields("first_child") Root firstRoot,
      @RequiresFields("value") Child childValue) {
    childValue = firstRoot.getFirstChild();
  }

  void needsFirstChild(@RequiresFields("first_child") Root root) {}

  void needsFirstChildValue(@RequiresFields("first_child.value") Root root) {}

  void checksMethodCalls(@RequiresFields("first_child") Root root) {
    needsFirstChild(root);
    needsFirstChildValue(root);
  }
}
