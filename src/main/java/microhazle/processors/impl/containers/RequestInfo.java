package microhazle.processors.impl.containers;

import microhazle.channels.abstrcation.hazelcast.Header;
import microhazle.channels.abstrcation.hazelcast.ITransport;

import java.io.Serializable;

/**
 * The class keeps information about request
 * @param <D>
 * @param <R>
 */
public class RequestInfo<D extends ITransport, R extends ITransport>  implements Serializable{
    public R getResponse() {
        return response;
    }


    public String getJobId() {

        return jobId;
    }

    private R response;
    String jobId=null;
    public D getData() {
        return data;
    }

    D      data;
    RequestInfo(String source,D data)
    {
        this.data = data;
        jobId = source;

    }
    void setResponse( R r)
    {
        response= r;
    }


}
