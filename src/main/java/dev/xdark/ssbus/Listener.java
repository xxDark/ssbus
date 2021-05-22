package dev.xdark.ssbus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** An annotation to mark methods as being event handler methods */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Listener {

  /** Define the priority of the handler */
  int priority() default 100;
}
