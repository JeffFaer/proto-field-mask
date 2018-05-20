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

  void cannotReassignExplicitlyAnnotatedVariable(
      @RequiresFields("first_child") Root firstRoot,
      @RequiresFields("second_child") Root secondRoot,
      Root unannotatedRoot,
      @RequiresFields("description") Child childDescription) {
    // BUG: Diagnostic contains: RHS has incompatible FieldMask
    firstRoot = secondRoot;

    // BUG: Diagnostic contains: RHS without FieldMask
    secondRoot = unannotatedRoot;

    // BUG: Diagnostic contains: RHS has incompatible FieldMask
    childDescription = firstRoot.getSecondChild();

    childDescription = firstRoot.getFirstChild();
    // BUG: Diagnostic contains: value
    childDescription.getValue();
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

    // BUG: Diagnostic contains: first_child
    tmp1.getFirstChild();
    // BUG: Diagnostic contains: second_child
    tmp2.getSecondChild();
  }

  void needsFirstChild(@RequiresFields("first_child") Root root) {}

  void needsFirstChildValue(@RequiresFields("first_child.value") Root root) {}

  void checksMethodCalls(@RequiresFields("first_child.description") Root root) {
    // BUG: Diagnostic contains: Argument has incompatible FieldMask
    RootUtils.needsFirstChild(root);
    // BUG: Diagnostic contains: Argument has incompatible FieldMask
    RootUtils.needsFirstChildValue(root);
  }

  class LocalVariables {
    void validatesFieldMask(@RequiresFields("first_child") Root firstRoot) {
      // BUG: Diagnostic contains: Invalid field path "invalid_field"
      @RequiresFields("invalid_field") Root root;

      root = firstRoot;
    }

    void localVariables(@RequiresFields({"first_child.value", "second_child"}) Root root) {
      // BUG: Diagnostic contains: RHS has incompatible FieldMask
      @RequiresFields("description") Child childDescription = root.getFirstChild();

      @RequiresFields("description") Child childDescription2;
      // BUG: Diagnostic contains: RHS has incompatible FieldMask
      childDescription2 = root.getFirstChild();

      childDescription = root.getSecondChild();
      // BUG: Diagnostic contains: value
      childDescription.getValue();
    }
  }
}
