package microhazle.impl.containers;

import microhazle.channels.abstrcation.hazelcast.ITransport;

import java.io.Serializable;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * The class describes job processing state and contains initial data as well as state of
 * a processing job
 *
 * @param <D>
 * @param <S>
 */
 class ProcessingState<D extends ITransport, S extends Serializable> implements Serializable {
     ReentrantLock lock = new ReentrantLock(true);
    D initial;
    public D getInitial() {
        return initial;
    }



    void mute(Function<S,S> mutator, S triggered,Runnable trigger)
    {
        S s=null;
        try
        {
           lock.lock();
           s=getState();
           if(mutator!=null)
           {
               s=mutator.apply(s);
           }
        }
        catch(Exception e)
        {

        }
        finally
        {
            state=s;
            lock.unlock();
            boolean run=false;
            if(trigger!=null) {
                if (s == null && triggered == null)
                    run = true;
                else {
                    if(s==null||trigger==null)
                        return;
                    if (s.equals(triggered)) {
                        run = true;
                    }
                }
                if (run) {
                    try {
                        trigger.run();

                    } catch (Throwable t) {

                    }

                }
            }



        }
    }


    public S getState() {
        return state;
    }

    S state;
    ProcessingState(D d)
    {
        initial=d;
    }

}
