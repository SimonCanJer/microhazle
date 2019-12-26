package microhazle.channels.abstrcation.hazelcast;

import java.io.Serializable;
import java.util.Stack;

public abstract class DTOMessageTransport<T extends ITransport> implements Serializable {
    protected T data;
    protected Stack<Header> stack = new Stack<>();
    protected Header header ;
    public Header getHeader()
    {
        return header;
    }
    public T getData() {
        return data;
    }
    void archive()
    {
        if(header!=null)
        {
            stack.add(header);

        }
        header=null;
    }
}
