package name.falgout.jeffrey.proto.fieldmask.usage;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Can be used to annotate a proto type with information about which fields will be accessed.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.PARAMETER)
public @interface RequiresFields {
  String[] value();
}
