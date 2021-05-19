package dev.xdark.ssbus;

import java.util.function.Consumer;

public interface BusRegistration<E> {

  RegisteredListener register(Consumer<E> listener, int priority);

  RegisteredListener register(Object handle);

  void bake();
}
