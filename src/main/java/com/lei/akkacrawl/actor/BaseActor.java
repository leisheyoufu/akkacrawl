package com.lei.akkacrawl.actor;

import akka.actor.ActorRef;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import akka.japi.Procedure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class BaseActor extends UntypedActor {
    protected Logger logger = LogManager.getLogger(this.getClass().getName());
    public final static String SHUTDOWN = "stop";
    public final static String EXPORT = "export";
    public final static String DONE = "done";
    public BaseActor() {
        ActorRef mediator = DistributedPubSub.get(getContext().system()).mediator();
        // subscribe to the topic named "terminator"
        mediator.tell(new DistributedPubSubMediator.Subscribe("terminator", getSelf()), getSelf());
    }
    protected Procedure<Object> shuttingDown = new Procedure<Object>() {
        @Override
        public void apply(Object message) {
            if (!(message instanceof Terminated)) {
                getSender().tell("service unavailable, shutting down", getSelf());
            } else if (message instanceof Terminated) {
                getContext().stop(getSelf());
            }
        }
    };
}
