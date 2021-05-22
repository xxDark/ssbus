package dev.xdark.ssbus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation to mark the method as virtual. By default, the bus threats all handler methods as
 * {@code final}, meaning that it can dispatch methods using {@code INVOKESPECIAL} instruction
 * instead of {@code INVOKEVIRTUAL}. For further information, visit JVM opcode specification
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Virtual {}
