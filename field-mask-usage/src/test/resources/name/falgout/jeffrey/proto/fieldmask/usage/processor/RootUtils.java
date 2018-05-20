package name.falgout.jeffrey.proto.fieldmask.usage.processor;

import name.falgout.jeffrey.proto.fieldmask.usage.RequiresFields;
import name.falgout.jeffrey.proto.fieldmask.usage.Test.Root;

class RootUtils {
  static void needsFirstChild(@RequiresFields("first_child") Root root) {}

  static void needsFirstChildValue(@RequiresFields("first_child.value") Root root) {}
}
