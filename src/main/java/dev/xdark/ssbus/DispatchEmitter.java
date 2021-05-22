package dev.xdark.ssbus;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.function.Consumer;

interface DispatchEmitter {

  void emitInfo(ClassVisitor visitor);

  int emitCode(String dispatcher, MethodVisitor visitor);

  void inject(Dispatcher<?> dispatcher);

  static DispatchEmitter forDirect(Object handle, Method m) {
    return new DirectDispatchEmitter(
        handle,
        m.getDeclaringClass(),
        m.getName(),
        MethodType.methodType(m.getReturnType(), m.getParameterTypes()).toMethodDescriptorString());
  }

  static DispatchEmitter forConsumer(Consumer<?> consumer) {
    return new ConsumerDispatchEmitter(consumer);
  }
}
