- The sample version of project is extracted  from previous versions, which have been designed to assist building distributed workflow ow integrating microservices, which are connected over Hazelcast. 
 - Hazelcast, in addition to cache functionality plays role of a Messaging. I am not about Publish/Subscribe, but I am about the IQueue Hazelcast API object. It is really something like LinkedBlockingQueue in Java, which serves communication between threads.
 The idea and the main motivation is to expand very simple and wellknown mechanism of asynchronous operations and threads communication over queues on to IPC, which communicate distributed services. The only difference is add(..) and take(..) are called by sender and consumer in different processes. Firthermore, regardles number of pollers over take(), only one will receive a message. So, number of (micro) service instance can process a same type of request- is not a natural load ballancing? Of coarse, it is not Kafka with its bandwith and peristed logs, but can be used for microservices horeography and orchestration.
 Hazelcast can be used as in process data grid, which does not require additional installs and, fortunatelly provides collections, caches, queues which are needed  to build distributed asynchronous/parallel processing, data caching, that is needed for microservices choreography and collaboration.
  - In this project workflow and furthering  event mechanism are realized using the IQueue distributed collection of Hazelcast with a name, which corresponds to type of processed events. The IQueue instances with the same name are created in any JVM of worklfow which is associated with related Hazelcast instance. So an add(..) in sender's JVM will be handled by a take(..) in consumer JVM.
 - Thus, workflow mechanism is leveraged by sending and consumning messages of predefinded types, using IQueue with a name which corresponds to the type, and, besides, by  a lthreads polling the queue.Due to the S (of SOLID) paradigm normally a process should consume a one type of events (have one listened IQueue). Number of destination IQueues,where event/request to be send to, is not limited. The important is that ratio of senders and consumers for IQueue is n:m . It means that number of instanes of JVM can (and must) listen for the same type of request. As it is mentioned above, this approach, together with the Hazelcast IQueue properties, provides  a simple and natural Load Ballancing.
 - In this realization we have used messaging (send-reply) communication: in a practice it means, that consumer anyway will reply to sender (it is not a pure distrubuted model). This way, any message is contained in a DTO. The DTO contains description of type of event (class usually)-Header, which encapsulated  main transportation information in the DTO, and history (Stack of headers).What it is for? It is for processor, which handles consumed request cans send own furthering request inside a worklfow implementation (as a subtask). The Stak of headers simplifies reply forming.
- In order to handle responses we have an additional IQueue in any message processing JVM: an own IQueue, which  is dedicated to get replies and messages, which  are directed to the concrete instance of JVM. Unlike IQueues, which are dedicated to listen incoming requests, the own IQueues exist with an unique name for any JVM, which is not related to type of message anyway. The IQeueue name is the sender address and is placed  into a dto object. before it have been put into a IQueue.
- The proposed mechanism, which connects services/miroservices by means of type orineted messaging  requires just to define name of a worklfow ( fedaration in our terms), register processors to handle a request /events type on consuming JVMs.  The realization of this mechanism provides an abstrcat class for processor implementation, which handles main non domain functionality: sends furthering request for subprocessing, handle reponses, exposes result. A concrete realization of any processor should just implement its business logic and declare requests, which will be sent for furthering processing while handling initial request. Any concrete processor lives in container, which communicates to underlaying Hazelcast system and linked to message routing channeles.
 - Any request processing is executed in a handling service, upon extracted from  a dedicated IQueue and  either result will be  returned or sub request will be sent. In order to serve many stage processing, container, keeps information about request, state of processing and uses the information for reply.
-  Declaration of request set,  which will be sent by a processors for furthering sub processing is necessary in order to verify are the related queues are  listened. Request will not be accepted if system does not see that it will be handled by a consumer instance (sure, it is an disadvantage, but unlike another message queues (like Kafka) messages are not persisted/logged and cache space sould not be grown)
-  Request send/Event firing mechanism is supported by a channels mechnanism. A channel, which a request will be transmisted over is obtained from API by name of the class of a concrete request. Concretelly a message sent is ended by IQueue.add(..). The channel also polls own IQueue of Hazelcast whic corresponds to sendng JVM and takes care about a result returned The send operation has number of implementations, incluidng callback on response  and Mono based asynchronous invok
-  Due to the S (of SOLID) paradigm,a process should consume a one type of request messaging (have one listened IQueue). Number of destination IQueues,where furthering request messages are to to be send to, is not limited. The important is that ratio of senders and consumers for IQueue is n:m . It means that number of instanes of JVM can (and must) listen for the same type of request. It is realization of the announced simple load ballancing- just start new process, which handles the same type of message.
- We have used currently,bidirectional (send-reply) communication: in practice it means that consumer anyway will reply to sender (it is not a pure distrubuted model). This way any message is contained in a DTO. The DTO contains description of type of event (class usually)-Header, which encapsulated  main transportation information, and history (Stack of headers).What it is for? It is because of a  processor, which handles consumed request, wil be able own subrequest request inside a worklfow implementation (as a subtask). It is  like a forked request, but without joint: any instance of JVM, which implements the mechanism has own (and unique IQueue). The Stack of headers, which is menioned above, simplifies reply forming and send.
 -  In order to handle responses we have an additional IQueue with a unique name : an own IQueue, which  is dedicated to get replies and messages, which  are directed to the concrete instance of JVM. Unlike IQueues, which are dedicated to listen incoming requests, the own IQueues exist with an unique name for any JVM/Service/ The IQeueue's  name is the sender address in the header of a DTO object, which envelops a request message. The reply will be put (add(..) to an instance of a  queue with this name.
 - The project includes API, which providess:
- create multi level request message processing mechanisms by providing request handlers per data class with fully automated request dispatch, simply build microservices just handling a type of request as message consumers. The undrlaying hazelcast native framework provides hidden instances discovery (by IQueue name); 
- convenient mechanisms for request send and reply, where routing and  LB are hidden behind the scene;
- create multi level workflow  as hierachic set of microservices with unidirectional asynchronous invokation  . The undrlaying hazelcast native framework provides hidden instances discovery (by IQueue name) and build in LB(just run new consumers)
- data impersonation and sharing among groups of services/microservices, for cross domain session build.
- registrtaion of any end point running on IP address of current JVM and port and populating it accross domain(ip address is obtained automatically, network interface are selected useing a privided filter/pattern) . It can be any type of endpoint.
As well, s we have remarked above, mechanism of such type might be simply used for creation of forked worklfow by microservices with aynchonous communication. where request is propagated  for a REST microservice as a facade of an application. In one or projects, I have used it together with zuul filters for gateway build without Eurica and it inherent problems of cluster IP setup.
