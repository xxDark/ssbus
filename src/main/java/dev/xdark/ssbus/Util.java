package dev.xdark.ssbus;

import java.util.concurrent.atomic.AtomicInteger;

final class Util {

  private static final AtomicInteger ID = new AtomicInteger();

  static String getInternalName(Class<?> c) {
    return c.getName().replace('.', '/');
  }

  static int nextId() {
    return ID.getAndIncrement();
  }
}
