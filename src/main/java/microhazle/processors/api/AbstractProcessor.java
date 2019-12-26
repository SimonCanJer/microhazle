package microhazle.processors.api;

import microhazle.channels.abstrcation.hazelcast.*;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class AbstractProcessor<T extends ITransport> {
    private Consumer<IReply> mSender;
    private Function<IMessage,String> mRequestSend;

    public abstract void process(T value);
    public <R extends IReply> void setResponseSender(Consumer<R> consumer)
    {
        mSender = (Consumer<IReply>) consumer;
    }
    public abstract Set<Class> announceRequestNeeded();
    public <M extends IMessage> void setRequestSender(Function<M,String> m)
    {
        mRequestSend= (Function<IMessage, String>) m;
    }
    public BiConsumer<String,? extends IReply> getResponseListener( )
    {
        return this::listener;

    }
    protected abstract <R extends IReply> void listener(String id,R t);


    protected <R extends IReply> void reply(R r)
    {
        mSender.accept(r);

    }

    protected <R extends IMessage> String  sendMessage(R message)
    {
        return mRequestSend.apply(message);
    }
    protected <R extends IReply> void  sendMessage(R message)
    {
        mSender.accept(message);
    }

}
