package dev.xdark.ssbus;

import java.util.List;

final class HandleRegisteredListener implements RegisteredListener {

    private final Bus<?> bus;
    private final List<DispatchInfo> infos;

    HandleRegisteredListener(Bus<?> bus, List<DispatchInfo> infos) {
        this.bus = bus;
        this.infos = infos;
    }

    @Override
    public void unregister() {
        List<DispatchInfo> infos = this.infos;
        bus.unregister(infos);
        infos.clear();
    }
}
