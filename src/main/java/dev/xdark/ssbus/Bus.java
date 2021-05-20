package dev.xdark.ssbus;

import java.util.*;
import java.util.function.Consumer;

public class Bus<E> implements BusRegistration<E> {

  private static final RegisteredListener NOOP_REGISTERED_LISTENER =
      new RegisteredListener() {
        @Override
        public void unregister() {}
      };
  private static final Dispatcher NOOP_DISPATCHER =
      new Dispatcher() {
        @Override
        public final void dispatch(Object event) {}
      };
  private static final Comparator<DispatchInfo> INFO_COMPARATOR =
      (o1, o2) -> o2.priority - o1.priority;
  private final List<DispatchInfo> infos = new ArrayList<>();
  private final Class<?> host;
  private final String internalName;
  private Dispatcher<E> dispatcher = NOOP_DISPATCHER;
  private boolean bakeOnRegistration = true;

  public Bus(Class<E> type) {
    this(type, Util.getInternalName(type));
  }

  public Bus(Class<?> host, Class<E> type) {
    this(host, Util.getInternalName(type));
  }

  private Bus(Class<?> host, String internalName) {
    this.host = host;
    this.internalName = internalName;
  }

  public final E unsafeFire(E event) {
    dispatcher.dispatch(event);
    return event;
  }

  public final E fire(E event) {
    try {
      dispatcher.dispatch(event);
    } catch (Throwable t) {
      exceptionCaught(t);
    }
    return event;
  }

  public final void unsafeFireAndForget(E event) {
    dispatcher.dispatch(event);
  }

  public final void fireAndForget(E event) {
    try {
      dispatcher.dispatch(event);
    } catch (Throwable t) {
      exceptionCaught(t);
    }
  }

  public final BusRegistration<E> newRegistration() {
    return new DelayedBusRegistration<>(this);
  }

  @Override
  public final RegisteredListener register(Consumer<E> listener, int priority) {
    DispatchInfo info = Util.createDispatchInfo(listener, priority);
    List<DispatchInfo> infos = this.infos;
    infos.add(info);
    infos.sort(INFO_COMPARATOR);
    if (bakeOnRegistration) {
      dispatcher = DispatcherGenerator.generateDispatcher(host, internalName, infos);
    }
    return new HandleRegisteredListener(this, Collections.singletonList(info));
  }

  @Override
  public final RegisteredListener register(Object handle) {
    List<DispatchInfo> infos = this.infos;
    int j = infos.size();
    Util.scan(infos, handle);
    int k = infos.size();
    if (j != k) {
      List<DispatchInfo> registered = new ArrayList<>(infos.subList(j, k));
      infos.sort(INFO_COMPARATOR);
      if (bakeOnRegistration) {
        dispatcher = DispatcherGenerator.generateDispatcher(host, internalName, infos);
      }
      return new HandleRegisteredListener(this, registered);
    }
    return NOOP_REGISTERED_LISTENER;
  }

  @Override
  public void bake() {
    List<DispatchInfo> infos = this.infos;
    if (infos.isEmpty()) {
      dispatcher = NOOP_DISPATCHER;
    } else {
      dispatcher = DispatcherGenerator.generateDispatcher(host, internalName, infos);
    }
  }

  public void setBakeOnRegistration(boolean bake) {
    this.bakeOnRegistration = bake;
  }

  public boolean isBakeOnRegistration() {
    return this.bakeOnRegistration;
  }

  void register(DispatchInfo infos) {
    this.infos.add(infos);
  }

  void registerAll(Collection<DispatchInfo> infos) {
    this.infos.addAll(infos);
  }

  void unregister(Collection<DispatchInfo> infos) {
    List<DispatchInfo> global = this.infos;
    if (global.removeAll(infos)) {
      if (global.isEmpty()) {
        dispatcher = NOOP_DISPATCHER;
      } else {
        global.sort(INFO_COMPARATOR);
        dispatcher = DispatcherGenerator.generateDispatcher(host, internalName, global);
      }
    }
  }

  protected void exceptionCaught(Throwable t) {}

  private static List<DispatchInfo> addInfo(List<DispatchInfo> infos, DispatchInfo info) {
    if (infos == null) {
      infos = new ArrayList<>();
    }
    infos.add(info);
    return infos;
  }
}
