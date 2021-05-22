package dev.xdark.ssbus;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

import static org.objectweb.asm.Opcodes.*;

final class DirectDispatchEmitter implements DispatchEmitter {

  private static final Unsafe UNSAFE = Util.unsafe();
  private final Object handle;
  private final String owner;
  private final String name;
  private final String descriptor;
  private final String handleField;
  private final boolean virtual;

  DirectDispatchEmitter(Object handle, Class<?> owner, String name, String descriptor, boolean virtual) {
    this.handle = handle;
    String $owner = owner.getName();
    this.owner = $owner.replace('.', '/');
    this.name = name;
    this.descriptor = descriptor;
    if (handle != null) {
      handleField = "__handle__" + name + "__" + Util.nextId();
    } else {
      handleField = null;
    }
    this.virtual = virtual;
  }

  @Override
  public void emitInfo(ClassVisitor visitor) {
    if (handle != null) {
      visitor
          .visitField(ACC_PRIVATE | ACC_FINAL, handleField, "Ljava/lang/Object;", null, null)
          .visitEnd();
    }
  }

  @Override
  public void inject(Dispatcher<?> dispatcher) {
    Object handle = this.handle;
    if (handle != null) {
      Field field;
      try {
        field = dispatcher.getClass().getDeclaredField(handleField);
      } catch (NoSuchFieldException ex) {
        throw new IllegalStateException("Missing handle field", ex);
      }
      Unsafe unsafe = UNSAFE;
      unsafe.putObject(dispatcher, unsafe.objectFieldOffset(field), handle);
    }
  }

  @Override
  public int emitCode(String dispatcher, MethodVisitor visitor) {
    Object handle = this.handle;
    int opcode = handle == null ? INVOKESTATIC : (virtual ? INVOKEVIRTUAL : INVOKESPECIAL);
    String owner = this.owner;
    int index = 0;
    if (handle != null) {
      visitor.visitVarInsn(ALOAD, index++);
      visitor.visitFieldInsn(GETFIELD, dispatcher, handleField, "Ljava/lang/Object;");
      visitor.visitTypeInsn(CHECKCAST, 'L' + owner + ';');
    }
    visitor.visitVarInsn(ALOAD, 1);
    visitor.visitMethodInsn(opcode, owner, name, descriptor, false);
    return index + 1;
  }
}
