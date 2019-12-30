package microhazle.examples;

import microhazle.channels.abstrcation.hazelcast.IMessage;

public class Capitalize implements IMessage{
    public String getData() {
        return data;
    }

    private final String data;

    public Capitalize(String data) {
        this.data=data;
    }
}
