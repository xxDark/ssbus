package dev.xdark.ssbus;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

final class Util {

  private static final Unsafe UNSAFE;
  private static final AtomicInteger ID = new AtomicInteger();

  static String getInternalName(Class<?> c) {
    return c.getName().replace('.', '/');
  }

  static int nextId() {
    return ID.getAndIncrement();
  }

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
