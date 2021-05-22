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

  /** @param type type of the event */
  public Bus(Class<E> type) {
    this(type, Util.getInternalName(type));
  }

  /**
   * @param host host class that will be used for dispatcher
   * @param type type of the event.
   */
  public Bus(Class<?> host, Class<E> type) {
    this(host, Util.getInternalName(type));
  }

  private Bus(Class<?> host, String internalName) {
    this.host = host;
    this.internalName = internalName;
  }

  /**
   * Fires an event without calling {@link Bus#exceptionCaught(Throwable)} if exception occurs
   *
   * @param event event to fire
   */
  public final <V extends E> V unsafeFire(V event) {
    dispatcher.dispatch(event);
    return event;
  }

  /**
   * Fires an event
   *
   * @param event event to fire
   */
  public final <V extends E> V fire(V event) {
    try {
      dispatcher.dispatch(event);
    } catch (Throwable t) {
      exceptionCaught(t);
    }
    return event;
  }

  /**
   * Fires an event without calling {@link Bus#exceptionCaught(Throwable)} if exception occurs
   *
   * @param event event to fire
   */
  public final void unsafeFireAndForget(E event) {
    dispatcher.dispatch(event);
  }

  /**
   * Fires an event
   *
   * @param event event to fire
   */
  public final void fireAndForget(E event) {
    try {
      dispatcher.dispatch(event);
    } catch (Throwable t) {
      exceptionCaught(t);
    }
  }

  /**
   * Returns new {@link BusRegistration} that may be used to generate multiple event handlers at
   * once
   *
   * @see Bus#setBakeOnRegistration(boolean)
   */
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

  /** Sets whether the dispatcher must be automatically baked on each handler (un)registration */
  public void setBakeOnRegistration(boolean bake) {
    this.bakeOnRegistration = bake;
  }

  /**
   * @return {@code true} if the bus must be automatically baked on each handler (un)registration
   */
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

  /**
   * Called whether exception occurs on event dispatching
   * Note that this method will NOT be called when unsafe methods are used
   */
  protected void exceptionCaught(Throwable t) {}
}
