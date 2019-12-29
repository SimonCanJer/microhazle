package microhazle.channels.abstrcation.hazelcast;

public class Error extends Throwable implements IReply {
   final private  String description;

    public String getDescription() {
        return description;
    }

    public Exception getError() {
        return error;
    }

    final private  Exception error;

    public Error(String description, Exception error) {
        this.description = description;
        this.error = error;
    }
}
