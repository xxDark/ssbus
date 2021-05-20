package dev.xdark.ssbus;

import java.util.List;

final class HandleRegisteredListener implements RegisteredListener {

    private final Bus<?> bus;
    private List<DispatchInfo> infos;

    HandleRegisteredListener(Bus<?> bus, List<DispatchInfo> infos) {
        this.bus = bus;
        this.infos = infos;
    }

    @Override
    public void unregister() {
        bus.unregister(infos);
        infos = null;
    }
}
