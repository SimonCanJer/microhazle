package microhazle.channels.concrete.hazelcast;

;
import com.hazelcast.core.IQueue;
import microhazle.channels.abstrcation.hazelcast.*;
import microhazle.channels.abstrcation.hazelcast.Error;
import org.apache.log4j.Logger;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Mono;

import java.rmi.UnknownHostException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * This class encapsulated information and skills about remote IQueue, where requests are
 * to be put. The class is an end point of related
 *  @see IProducerChannel
 *  an handles a send request.
 *  The IQueue
 * @see #q
 * is provided by Hazelcast mechanism when destination is registered anywhere accross group.
 * Only after the IQueue is provided, the channel is ready for use. The class generates notification
 * upon queue is set, using a callback
 * @see #setQ(IQueue)  and
 * @see #notifier
 * @param <T> tye of request
 */
class DestinationQ<T extends ITransport> implements IProducerChannel<T> {
    private final Logger logger;
    private final String strReplyQ;
    IQueue<DTOMessageTransport<T>> q;
    Consumer<IProducerChannel<T>> notifier;
    BiConsumer<String,Consumer> registrar;
    DestinationQ(BiConsumer<String,Consumer> registrar, String strReplyQ, Logger logger)
    {
        this.registrar=registrar;
        this.logger=logger;
        this.strReplyQ= strReplyQ;
    }
    void setQ(IQueue<DTOMessageTransport<T>> q) {
        this.q = q;
        if (notifier != null) {
            logger.trace("destination queue is connected now tp channel "+q.getName());
            notifier.accept(this);
        }
    }

    @Override
    public boolean isConnected() {
        return q!=null;
    }

    @Override
    public  <R extends IReply> String post(DTOMessageTransport<T> message, Consumer<DTOReply<R>> listener) throws UnknownHostException {
        message.getHeader().setSource(strReplyQ);
        if (q != null) {
            if(listener!=null) {
               registrar.accept(message.getHeader().getId(),listener);
            }
                //mapPendingListeners.put(message.getHeader().getId(),listener);
            q.add(message);
            if(q.size()>450)
            {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return message.getHeader().getId();
        }
        else {
            logger.error("Unknown host for message");
            throw new UnknownHostException(message.getHeader().getDestination());
        }

    }

    @Override
    public <R extends IReply> Mono<R> post(DTOMessageTransport<T> message) throws UnknownHostException {
        Subscriber<? super R>[] imported = new Subscriber[1];
        boolean [] ignore=new boolean[]{false};
        Publisher<? extends IReply> p= new Publisher<IReply>() {

            @Override
            public void subscribe(Subscriber<? super IReply> subscriber) {
                imported[0]=subscriber;
                imported[0].onSubscribe(new Subscription() {
                    @Override
                    public void request(long l) {

                    }

                    @Override
                    public void cancel() {
                        ignore[0]=true;

                    }
                });
            }
        };
        Mono<? extends IReply> mono= Mono.from(p);
        post(message,( r)->{if(ignore[0]) return;if(
                r.getData() instanceof microhazle.channels.abstrcation.hazelcast.Error) imported[0].onError((Error)r.getData());logger.trace("subscriber: onNext");
            imported[0].onNext((R)r.getData());});
        return (Mono<R>) mono;
    }

    @Override
    public <R extends IReply> Future<R> send(DTOMessageTransport<T> message) throws UnknownHostException {
        CompletableFuture<R> res= new CompletableFuture<>();
        post(message,(r)->{res.complete((R) r.getData());});
        return res;
    }


}
