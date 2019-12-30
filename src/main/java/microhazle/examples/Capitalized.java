package microhazle.examples;

import microhazle.channels.abstrcation.hazelcast.IReply;

public class Capitalized implements IReply {
    public String getData() {
        return data;
    }

    private final String data;

    public Capitalized(String data) {
        this.data = data;
    }
}
