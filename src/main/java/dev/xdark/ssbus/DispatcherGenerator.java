package dev.xdark.ssbus;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import sun.misc.Unsafe;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;

final class DispatcherGenerator {

  private static final Unsafe UNSAFE = Util.unsafe();

  static <E> Dispatcher<E> generateDispatcher(
      Class<?> host, String type, List<DispatchInfo> infos) {
    int i = 0, j = infos.size();
    ClassWriter writer = new ClassWriter(0);
    String name = getClassName();
    ClassInfo info = new ClassInfo(writer, V1_1, ACC_FINAL, name, Util.BRIDGE_NAME);
    info.interfaces.add("dev/xdark/ssbus/Dispatcher");
    for (; i < j; i++) {
      infos.get(i).emitter.emitInfo(writer);
    }
    String superName = info.superName;
    writer.visit(
        V1_8, ACC_FINAL, name, null, superName, info.interfaces.toArray(new String[0]));
    MethodVisitor init = writer.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
    init.visitCode();
    init.visitVarInsn(ALOAD, 0);
    init.visitMethodInsn(INVOKESPECIAL, superName, "<init>", "()V", false);
    init.visitInsn(RETURN);
    init.visitMaxs(1, 1);
    init.visitEnd();
    MethodVisitor invoke =
        writer.visitMethod(ACC_PUBLIC | ACC_FINAL, "dispatch", "(Ljava/lang/Object;)V", null, null);
    invoke.visitCode();
    invoke.visitVarInsn(ALOAD, 1);
    invoke.visitTypeInsn(CHECKCAST, type);
    invoke.visitVarInsn(ASTORE, 1);
    int stackSize = 0;
    for (i = 0; i < j; i++) {
      stackSize = Math.max(stackSize, infos.get(i).emitter.emitCode(name, invoke));
    }
    invoke.visitInsn(RETURN);
    invoke.visitMaxs(stackSize, 2);
    invoke.visitEnd();
    writer.visitEnd();
    byte[] bytes = writer.toByteArray();
    Class<?> klass = UNSAFE.defineAnonymousClass(host, bytes, null);
    Dispatcher<E> dispatcher;
    try {
      dispatcher = (Dispatcher<E>) klass.getConstructor().newInstance();
    } catch (InstantiationException
        | IllegalAccessException
        | InvocationTargetException
        | NoSuchMethodException ex) {
      throw new IllegalStateException("Could not create dispatcher instance", ex);
    }
    for (i = 0; i < j; i++) {
      DispatchEmitter emitter = infos.get(i).emitter;
      emitter.inject(dispatcher);
    }
    return dispatcher;
  }

  private static String getClassName() {
    return "dev/xdark/ssbus/GeneratedDispatcher_" + Util.nextId();
  }
}
