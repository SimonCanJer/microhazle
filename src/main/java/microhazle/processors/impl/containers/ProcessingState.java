package microhazle.processors.impl.containers;

import microhazle.channels.abstrcation.hazelcast.ITransport;

import java.io.Serializable;

/**
 * The class describes job processing state and contains initial data as well as state of
 * a processing job
 *
 * @param <D>
 * @param <S>
 */
 class ProcessingState<D extends ITransport, S extends Serializable> {
    public D getInitial() {
        return initial;
    }

    public void setState(S state) {

        this.state = state;
    }

    D initial;

    public Serializable getState() {
        return state;
    }

    Serializable state;
    ProcessingState(D d)
    {
        initial=d;
    }

}
