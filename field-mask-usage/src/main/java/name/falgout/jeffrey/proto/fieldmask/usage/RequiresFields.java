package name.falgout.jeffrey.proto.fieldmask.usage;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Can be used to annotate a proto type with information about which fields will be accessed.
 */
@Retention(RetentionPolicy.SOURCE)
public @interface RequiresFields {
  String[] value();
}
