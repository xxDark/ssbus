package dev.xdark.ssbus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

final class DelayedBusRegistration<E> implements BusRegistration<E> {

  private final Bus<?> bus;
  private boolean dirty;

  DelayedBusRegistration(Bus<?> bus) {
    this.bus = bus;
  }

  @Override
  public RegisteredListener register(Consumer<E> listener, int priority) {
    DispatchInfo info = Util.createDispatchInfo(listener, priority);
    Bus<?> bus = this.bus;
    bus.register(info);
    dirty = true;
    return new HandleRegisteredListener(bus, Collections.singletonList(info));
  }

  @Override
  public RegisteredListener register(Object handle) {
    List<DispatchInfo> infos = new ArrayList<>();
    Util.scan(infos, handle);
    Bus<?> bus = this.bus;
    bus.registerAll(infos);
    dirty |= !infos.isEmpty();
    return new HandleRegisteredListener(bus, infos);
  }

  @Override
  public void bake() {
    if (dirty) {
      bus.bake();
    }
  }
}
