package dev.xdark.ssbus;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Consumer;

public class Bus<E> {

  private static final RegisteredListener NOOP_REGISTERED_LISTENER =
      new RegisteredListener() {
        @Override
        public void unregister() {}
      };
  private static final Dispatcher NOOP_DISPATCHER =
      new Dispatcher() {
        @Override
        public void dispatch(Object event) {}
      };
  private static final Comparator<DispatchInfo> INFO_COMPARATOR =
      (o1, o2) -> o2.priority - o1.priority;
  private final List<DispatchInfo> infos = new ArrayList<>();
  private final Class<?> host;
  private final String internalName;
  private Dispatcher<E> dispatcher = NOOP_DISPATCHER;

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

  public final RegisteredListener register(Consumer<E> listener, int priority) {
    DispatchInfo info = new DispatchInfo(DispatchEmitter.forConsumer(listener), priority);
    List<DispatchInfo> infos = this.infos;
    infos.add(info);
    infos.sort(INFO_COMPARATOR);
    dispatcher = DispatcherGenerator.generateDispatcher(host, internalName, infos);
    return new HandleRegisteredListener(this, Collections.singletonList(info));
  }

  public final RegisteredListener register(Object handle) {
    Class<?> owner = unmaskOwner(handle);
    List<DispatchInfo> infos = this.infos;
    Object mask = maskHandle(handle);
    List<DispatchInfo> registered = null;
    for (Method m : owner.getDeclaredMethods()) {
      Listener anno = m.getDeclaredAnnotation(Listener.class);
      if (anno == null) continue;
      boolean virtual = !Modifier.isStatic(m.getModifiers());
      if (virtual == (handle == owner)) {
        continue;
      }
      DispatchInfo info = new DispatchInfo(DispatchEmitter.forDirect(mask, m), anno.priority());
      infos.add(info);
      registered = addInfo(registered, info);
    }
    if (registered != null) {
      infos.sort(INFO_COMPARATOR);
      dispatcher = DispatcherGenerator.generateDispatcher(host, internalName, infos);
      return new HandleRegisteredListener(this, registered);
    }
    return NOOP_REGISTERED_LISTENER;
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

  private static Object maskHandle(Object handle) {
    return handle instanceof Class ? null : handle;
  }

  private static Class<?> unmaskOwner(Object handle) {
    if (handle instanceof Class) {
      return (Class<?>) handle;
    }
    return handle.getClass();
  }

  private static List<DispatchInfo> addInfo(List<DispatchInfo> infos, DispatchInfo info) {
    if (infos == null) {
      infos = new ArrayList<>();
    }
    infos.add(info);
    return infos;
  }
}
