package microhazle.channels.abstrcation.hazelcast;

public class DTOReply<T extends IReply> extends DTOMessageTransport<T> {

    public DTOReply(T reply,DTOMessageTransport<? extends ITransport> src) {
           src.archive();
           data=reply;
           header = src.stack.pop();
           stack= src.stack;
    }
    DTOReply(T data, DTOReply<? extends IReply > current)
    {
        this.data = data;
        header = current.stack.pop();
        stack=current.stack;

    }
    public  <D extends IReply> DTOReply<D> continueReply(D d)
    {
        if(stack.size()==0)
            return null;
        return new DTOReply<D>(d,this);

    }
}
