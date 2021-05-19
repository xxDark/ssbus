package dev.xdark.ssbus;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

final class UnsafeUtil {

  private static final Unsafe UNSAFE;

  static Unsafe unsafe() {
    return UNSAFE;
  }

  static {
    try {
      Field f = Unsafe.class.getDeclaredField("theUnsafe");
      f.setAccessible(true);
      UNSAFE = (Unsafe) f.get(null);
    } catch (IllegalAccessException | NoSuchFieldException ex) {
      throw new ExceptionInInitializerError(ex);
    }
  }
}
