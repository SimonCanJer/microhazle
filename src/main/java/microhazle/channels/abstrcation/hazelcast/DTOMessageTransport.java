package microhazle.channels.abstrcation.hazelcast;

import java.io.Serializable;
import java.util.Stack;

/**
 * The base class of data transportation object which contains
 * instanc of transpaorted, curent header and stack of previos transportations
 * wich are in a workflow chain, which the current transport continues.
 * @param <T>
 */
public abstract class DTOMessageTransport<T extends ITransport> implements Serializable {
    protected T data;
    protected Stack<Header> stack = new Stack<>();
    protected Header header ;
    public Header getHeader()
    {
        return header;
    }

    /**
     * data getter
     * @return data
     */
    public T getData() {
        return data;
    }

    /**
     * makes ready to transportation, by putting
     * header to stack
     */
    void handsOff()
    {
        if(header!=null)
        {
            stack.add(header);

        }
        header=null;
    }

}
