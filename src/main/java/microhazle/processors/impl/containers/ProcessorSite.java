package microhazle.processors.impl.containers;

import microhazle.channels.abstrcation.hazelcast.*;
import microhazle.processors.api.AbstractProcessor;
import org.apache.log4j.Logger;

import java.lang.reflect.Method;
import java.rmi.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * The class manage lifecycle of a processor, providing
 * request/reply envelope into DTO, trailing and hiding behind the scene
 * send, receive
 * interaction
 * @param <T>
 */
public class ProcessorSite<T extends IMessage> implements IMessageConsumer {
// this listener dedicated to listen response to a message sent
    private final BiConsumer<String, IReply> responseListener;
    IRouter router;
    Logger logger=Logger.getLogger(this.getClass());
    AbstractProcessor<T> processor;
    Class mClass=IMessage.class;;

// keeps incoming transport object around processing
    ThreadLocal<DTOMessageTransport<? extends ITransport>> incoming= new ThreadLocal<>();
    public ProcessorSite(AbstractProcessor<T> p, IRouter router)
    {

        processor =p;
        logger.trace("processor instance wrapped "+p.getClass());
        this.router=router;
        responseListener = (BiConsumer<String, IReply>) p.getResponseListener();
        p.setRequestSender(this::send);
        p.setResponseSender(this::sendResult);
        Method[] methods =p.getClass().getDeclaredMethods();
        for(Method m:methods)
        {
            if(m.getName().equals("process"))
            {
                if(m.getParameterTypes().length==1)
                {
                    /*
                     * only extending class considered as recent(note, a complicated inheritance can be)
                     */
                    if(mClass.isAssignableFrom(m.getParameterTypes()[0])) {
                        logger.trace("added processor for " + m.getParameterTypes()[0]);
                        mClass = m.getParameterTypes()[0];
                    }
                }

            }
        }
   }

    /**
     * envelops into DTO and send message to destination
     * @param r              request object
     * @param <R>            request type
     * @return               identifier of current send. Delegates the identifier creation
     *
     */
    <R extends IMessage> String send(R r)
    {

        IProducerChannel<R> channel=router.getChannel((Class<R>)r.getClass(),null);
        DTOMessageTransport<? extends ITransport> dto = incoming.get();
        try {
            return channel.post(new DTOMessage<R>(r,dto),this::onReply);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
         return null;

    }

    /**
     * handles reply to message has been sent
     * @param reply DTO of reply
     * @param <R> reply type
     */
    <R extends IReply>void onReply(DTOReply<R> reply)
    {
        incoming.set(reply);
        R data=reply.getData();
        responseListener.accept(reply.getHeader().getId(),data);

    }

    /**
     * sends result to destination, using
     * @see DTOReply
     * @param r       returned result
     * @param <R>     type of returned result
     */
    <R extends IReply> void sendResult(R r)
    {
        DTOMessageTransport<? extends ITransport> transport=incoming.get();
        DTOReply<R> send=null;
        if(transport instanceof DTOReply)
        {
            DTOReply<? extends IReply> reply= (DTOReply<R>) transport;
            send=reply.continueReply(r);
            router.reply(send);
            return ;

        }
        else
            if(transport instanceof  DTOMessage)
            {
               send = new DTOReply<R>(r,transport);
            }
            if(send!=null)
            router.reply(send);

    }

    /**
     * performs primary handling incoming request
     *
     */
    @Override
    public void handle(DTOMessageTransport<? extends ITransport> dto) {
        incoming.set(dto);
        processor.process((T) dto.getData());
        incoming.set(null);

    }


    @Override
    public Set<String> getHandledMessageClasses() {
        List<String> list= Arrays.asList(new String[]{mClass.getName()} );
        return new HashSet<>(list);
    }
}
