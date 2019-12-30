package microhazle.channels.abstrcation.hazelcast;

/**
 * The class defines transport object for message reply BACK TO SENDER
 * @param <T>
 */
public class DTOReply<T extends IReply> extends DTOMessageTransport<T> {
    /**
     * constrcats reply to a message
     * @param payload value of reply
     * @param src source message
     */
    public DTOReply(T payload,DTOMessageTransport<? extends ITransport> src) {
        // prepare history of messaging
           src.handsOff();
           data=payload;
           header = src.stack.pop();/// set current header from history (for sender)
           stack= src.stack;
    }

    /**
     * prepare reply data transport from previous transport.
     * Uses sequency of reply from stack of pevious replay
     * @param data
     * @param current
     */
    DTOReply(T data, DTOReply<? extends IReply > current)
    {
        this.data = data;
        header = current.stack.pop();
        stack=current.stack;

    }

    /**
     * generates reply by drilling down sequence of messages
     * @param d
     * @param <D>
     * @return
     */
    public  <D extends IReply> DTOReply<D> continueReply(D d)
    {
        if(stack.size()==0)
            return null;
        if(d==null)
            d= (D) getData();
        return new DTOReply<D>(d,this);

    }
    public boolean canBePropagated()
    {
        return stack.size()>0;

    }
}
