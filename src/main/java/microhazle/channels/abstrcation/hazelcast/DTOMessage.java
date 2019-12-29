package microhazle.channels.abstrcation.hazelcast;

/**
 * DTO for messsage
 * Message's headers must always have destimation matching name of queue accociated with node of
 * workflow where the object is sent  to
 * @param <T>
 */
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
        m.handsOff();
        stack.addAll(m.stack);

    }

    /**
     * This is constructing object  while building chin of messages for follwing reply
     * @param call
     * @param message
     */
    public DTOMessage(T call,DTOMessageTransport<? extends ITransport> message) {
        this(call);
        message.handsOff();
        stack.addAll(message.stack);
    }
}
