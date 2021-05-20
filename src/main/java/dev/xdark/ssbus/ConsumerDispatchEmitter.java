package dev.xdark.ssbus;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.function.Consumer;

import static org.objectweb.asm.Opcodes.*;

final class ConsumerDispatchEmitter implements DispatchEmitter {

  private static final Unsafe UNSAFE = Util.unsafe();
  private final String consumerField = "__consumer__" + Util.nextId();
  private final Consumer<?> consumer;

  ConsumerDispatchEmitter(Consumer<?> consumer) {
    this.consumer = consumer;
    Class<?> type = consumer.getClass();
  }

  @Override
  public void emitInfo(ClassVisitor visitor) {
    visitor
        .visitField(
            ACC_PRIVATE | ACC_FINAL, consumerField, "Ljava/util/function/Consumer;", null, null)
        .visitEnd();
  }

  @Override
  public void inject(Dispatcher<?> dispatcher) {
    Field field;
    try {
      field = dispatcher.getClass().getDeclaredField(consumerField);
    } catch (NoSuchFieldException ex) {
      throw new IllegalStateException("Missing consumer field", ex);
    }
    Unsafe unsafe = UNSAFE;
    unsafe.putObject(dispatcher, unsafe.objectFieldOffset(field), consumer);
  }

  @Override
  public int emitCode(String dispatcher, MethodVisitor visitor) {
    visitor.visitVarInsn(ALOAD, 0);
    visitor.visitFieldInsn(GETFIELD, dispatcher, consumerField, "Ljava/util/function/Consumer;");
    visitor.visitVarInsn(ALOAD, 1);
    visitor.visitMethodInsn(
        INVOKEINTERFACE, "java/util/function/Consumer", "accept", "(Ljava/lang/Object;)V", true);
    return 2;
  }
}
