package dev.xdark.ssbus;

import java.util.function.Consumer;

public interface BusRegistration<E> {

  /**
   * Registers new handler with given {@code listener} and {@code priority}
   *
   * @param listener listener that will handle an event
   * @param priority priority of the listener
   * @return {@link RegisteredListener} that may be used to unregister the handler
   */
  RegisteredListener register(Consumer<E> listener, int priority);

  /**
   * Registers all the handlers in the given {@code handle} object If the given {@code handle} is a
   * class, all static methods are scanned, otherwise, only virtual methods are
   *
   * @param handle handle to scan methods from
   * @return {@link RegisteredListener} that may be used to unregister the handler
   */
  RegisteredListener register(Object handle);

  /** Bakes the bus, causing dispatcher to re-generate */
  void bake();
}
