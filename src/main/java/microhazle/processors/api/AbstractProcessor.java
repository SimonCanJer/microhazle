package microhazle.processors.api;

import microhazle.channels.abstrcation.hazelcast.*;
import microhazle.impl.containers.ProcessorSite;

import java.io.Serializable;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * This is a sceleton class for a message processor, declares lifetime functionality
 * implements serving functions and contains main fields.
 * @param <T>
 */
public abstract class AbstractProcessor<T extends ITransport,S extends Serializable> {
    private Consumer<IReply> mSender;
    private Function<IMessage,String> mRequestSend;
    private IJobContext dataContext;
    public void setDataContext(IJobContext context)
    {
        dataContext= context;
    }
    public IJobContext<T,S> jobContext()
    {
        return dataContext;
    }

    /**
     * called by container .ProcessorSite as entry point to process
     * incoming message
     * @param value
     * @see ProcessorSite
     */
    public abstract void process(T value);

    /**
     * sets consumer to be used fro response senidng. ProcessorSite calls this method to
     * set
     * @param consumer the imported consumer
     * @param <R>
     */
    public <R extends IReply> void setResponseSender(Consumer<R> consumer)
    {
        mSender = (Consumer<IReply>) consumer;
    }

    /**
     * declares clases of additional request messages are
     * needed to send in order to perform processing
     * @return
     */
    public abstract Set<Class> announceRequestNeeded();

    /**
     * setter for request send to additional services to perform
     * processing
     * @param  sender announced request sender
     * @param <M>
     */
    public <M extends IMessage> void setRequestSender(Function<M,String> sender)
    {
        mRequestSend= (Function<IMessage, String>) sender;
    }

    /**
     * getter to export response listener.
     *
     * @return
     */
    public BiConsumer<String,? extends IReply> getResponseListener( )
    {
        return this::listener;

    }

    /**
     * response listener to accept incoming responses to request messages sent
     * @param id id of responded request
     * @param t  the response payload
     * @param <R>
     */
    protected abstract <R extends IReply> void listener(String id,R t);

    /**
     *  the method commences reply and to be called
     *  during execution either of process or listen method
     * @param r the reply
     * @param <R>
     */
    protected <R extends IReply> void reply(R r)
    {
        mSender.accept(r);

    }

    protected <R extends IMessage> String sendRequestMessage(R message)
    {
        return mRequestSend.apply(
                message);
    }
    protected <R extends IReply> void replyMessage(R message)
    {
        mSender.accept(message);
    }

}
