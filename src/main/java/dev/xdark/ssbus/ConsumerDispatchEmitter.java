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
  private final boolean synthetic;
  private final String invoker;
  private final String descriptor;

  ConsumerDispatchEmitter(Consumer<?> consumer) {
    this.consumer = consumer;
    Class<?> type = consumer.getClass();
    boolean synthetic = type.isSynthetic();
    this.synthetic = synthetic;
    String invoker = synthetic ? "java/util/function/Consumer" : Util.getInternalName(type);
    this.invoker = invoker;
    descriptor = 'L' + invoker + ';';
  }

  @Override
  public void emitInfo(ClassVisitor visitor) {
    visitor.visitField(ACC_PRIVATE | ACC_FINAL, consumerField, descriptor, null, null).visitEnd();
  }

  @Override
  public void inject(Dispatcher<?> dispatcher) {
    // Inject consumer field
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
    visitor.visitFieldInsn(GETFIELD, dispatcher, consumerField, descriptor);
    visitor.visitVarInsn(ALOAD, 1);
    boolean synthetic = this.synthetic;
    visitor.visitMethodInsn(
        synthetic ? INVOKEINTERFACE : INVOKEVIRTUAL,
        invoker,
        "accept",
        "(Ljava/lang/Object;)V",
        synthetic);
    return 2;
  }
}
