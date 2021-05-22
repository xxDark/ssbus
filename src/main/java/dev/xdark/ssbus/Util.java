package dev.xdark.ssbus;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.objectweb.asm.Opcodes.*;

final class Util {

  private static final Unsafe UNSAFE;
  private static final AtomicInteger ID = new AtomicInteger();
  static final String BRIDGE_NAME =
      "sun/reflect/MagicAccessorBridge"
          + /* use timestamp because some other library may attempt to define the class with the same name, we don't want that */ System
              .currentTimeMillis();

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

  private static byte[] generateBridge() {
    ClassWriter writer = new ClassWriter(0);
    writer.visit(V1_8, ACC_PUBLIC, BRIDGE_NAME, null, "sun/reflect/MagicAccessorImpl", null);
    MethodVisitor init = writer.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
    init.visitCode();
    init.visitVarInsn(ALOAD, 0);
    init.visitMethodInsn(INVOKESPECIAL, "sun/reflect/MagicAccessorImpl", "<init>", "()V", false);
    init.visitInsn(RETURN);
    init.visitMaxs(1, 1);
    init.visitEnd();
    writer.visitEnd();
    return writer.toByteArray();
  }

  static {
    Unsafe unsafe;
    try {
      Field f = Unsafe.class.getDeclaredField("theUnsafe");
      f.setAccessible(true);
      UNSAFE = unsafe = (Unsafe) f.get(null);
    } catch (IllegalAccessException | NoSuchFieldException ex) {
      throw new ExceptionInInitializerError(ex);
    }
    // Now, let's try to generate bridge class inside of bootstrap class loader
    try {
      byte[] bytes = generateBridge();
      unsafe.defineClass(null, bytes, 0, bytes.length, null, null);
    } catch (Throwable t) {
      throw new ExceptionInInitializerError(t);
    }
  }
}
