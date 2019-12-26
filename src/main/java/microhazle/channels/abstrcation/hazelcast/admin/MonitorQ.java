package microhazle.channels.abstrcation.hazelcast.admin;

import microhazle.channels.abstrcation.hazelcast.IMessage;

public class MonitorQ implements IMessage {
    public boolean isOn() {
        return on;
    }

    final boolean on;

    public MonitorQ(boolean on) {
        this.on = on;
    }
}
