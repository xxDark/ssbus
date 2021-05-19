package dev.xdark.ssbus;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

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

  static DispatchInfo createDispatchInfo(Consumer<?> consumer, int priority) {
    return new DispatchInfo(DispatchEmitter.forConsumer(consumer), priority);
  }

  static void scan(List<DispatchInfo> infos, Object handle) {
    Class<?> owner = unmaskOwner(handle);
    Object mask = maskHandle(handle);
    for (Method m : owner.getDeclaredMethods()) {
      Listener anno = m.getDeclaredAnnotation(Listener.class);
      if (anno == null) continue;
      boolean virtual = !Modifier.isStatic(m.getModifiers());
      if (virtual == (handle == owner)) {
        continue;
      }
      DispatchInfo info = new DispatchInfo(DispatchEmitter.forDirect(mask, m), anno.priority());
      infos.add(info);
    }
  }

  private static Object maskHandle(Object handle) {
    return handle instanceof Class ? null : handle;
  }

  private static Class<?> unmaskOwner(Object handle) {
    if (handle instanceof Class) {
      return (Class<?>) handle;
    }
    return handle.getClass();
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
