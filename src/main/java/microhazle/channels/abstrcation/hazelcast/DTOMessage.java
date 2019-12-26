package microhazle.channels.abstrcation.hazelcast;

public class DTOMessage<T extends IMessage> extends DTOMessageTransport<T> {



    public DTOMessage(T data,String dest)
    {
        this(data,"",dest);
    }
    public DTOMessage(T data)
    {
        this(data,"",data.getClass().getName());
    }
    public DTOMessage(T data ,String src,String dest)
    {
        this.data = data;
        if(header != null)
        {
            stack.add(header);
        }
        header = new  Header(src, dest);

    }
    public DTOMessage(T data ,DTOMessageTransport<? extends ITransport> m,String dest)
    {
        this(data, dest);
        m.archive();
        stack.addAll(m.stack);

    }

    public DTOMessage(T call,DTOMessageTransport<? extends ITransport> message) {
        this(call);
        message.archive();
        stack.addAll(message.stack);
    }
}
