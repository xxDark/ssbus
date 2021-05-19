# ssbus - Supersonic, lightweight event bus

Example usage:

```Java
public class HelloBusTest {

    private static final int N = Integer.MAX_VALUE;

    public static void main(String[] args) {
        Bus<Event> bus = new Bus<>(Event.class);
        bus.register(
                e -> {
                    e.message = "Hello from lambdas!";
                },
                100);
        bus.register(EventListener.class);
        long now = System.currentTimeMillis();
        for (int i = 0; i < N; i++) {
            Event e = new Event("Hello, World!");
            bus.unsafeFireAndForget(e);
        }
        System.out.println(System.currentTimeMillis() - now);
    }

    public static final class EventListener {
        @Listener
        public static void onEvent(Event e) {
            e.message = "Hello from hand-crafted classes!";
        }
    }

    public static final class Event {
        String message;

        public Event(String message) {
            this.message = message;
        }
    }
}
```

If someone is able to provide better JMH benchmarks, I will appreciate that.