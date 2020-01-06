package microhazle.channels.concrete.hazelcast;

import com.hazelcast.core.IQueue;
import microhazle.channels.abstrcation.hazelcast.DTOMessageTransport;
import microhazle.channels.abstrcation.hazelcast.IMessageConsumer;
import microhazle.channels.abstrcation.hazelcast.ITransport;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * The class is satelite of Hazelcast provider and used by it.
 * Moved outside ONLY becaause of  HazelcastChannelProvider contains too many classes definition
 * @see HazelcastChannelProvider
 */
class PoolingThreads {
    IQueue<DTOMessageTransport<? extends ITransport>> mQ;
    IMessageConsumer consumer = null;
    final private Thread[] mThreads;
    CountDownLatch latch;
    PoolingThreads(IMessageConsumer consumer, String strName, IQueue<DTOMessageTransport<? extends ITransport>> q, int threads) {
        this.consumer = consumer;
        latch= new CountDownLatch(threads);
        mQ = q;
        this.mThreads = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            mThreads[i] = new Thread(this::execute);
            mThreads[i].start();
        }
    }
    void join()
    {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(mThreads==null)
            return ;
        for(Thread t:mThreads)
        {
            if(t!=null)
            {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void execute() {
        while (true) {
            try {
                DTOMessageTransport<? extends ITransport> take=mQ.take();
                if(take==null)
                    continue;
                System.out.println(take);
                if(take instanceof HazelcastChannelProvider.EndOfQPooling) {
                    System.out.println("breaking");
                    break;
                }
                consumer.handle(take);
            } catch (Throwable e) {
                e.printStackTrace();
                break;
            }
        }
        latch.countDown();
    }

    void end() {
        for (Thread t: mThreads) {
            mQ.add(new HazelcastChannelProvider.EndOfQPooling<>());
        }
       try {
            latch.await(10000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
