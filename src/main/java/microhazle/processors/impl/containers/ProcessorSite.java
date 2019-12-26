package microhazle.processors.impl.containers;

import microhazle.channels.abstrcation.hazelcast.*;
import microhazle.processors.api.AbstractProcessor;

import java.lang.reflect.Method;
import java.rmi.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

public class ProcessorSite<T extends IMessage> implements IMessageConsumer {

    private final BiConsumer<String, IReply> responseListener;
    IRouter router;
    AbstractProcessor<T> processor;
    Class mClass;


    ThreadLocal<DTOMessageTransport<? extends ITransport>> incoming= new ThreadLocal<>();
    public ProcessorSite(AbstractProcessor<T> p, IRouter router)
    {
        processor =p;
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
                    mClass= m.getParameterTypes()[0];
                }

            }
        }


    }
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
    <R extends IReply>void onReply(DTOReply<R> reply)
    {
        incoming.set(reply);
        R data=reply.getData();
        responseListener.accept(reply.getHeader().getId(),data);

    }
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

    @Override
    public void handle(DTOMessageTransport<? extends ITransport> dto) {
        incoming.set(dto);
        processor.process((T) dto.getData());

    }

    @Override
    public Set<String> getHandledMessageClasses() {

        List<String> list= Arrays.asList(new String[]{mClass.getName()} );
        return new HashSet<>(list);
    }
}
