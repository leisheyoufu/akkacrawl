package com.lei.akkacrawl.actor;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import com.lei.akkacrawl.AsyncHttpHandler;

import java.util.concurrent.atomic.AtomicInteger;

public class TerminateActor extends BaseActor {
    // NOTE(leisheyoufu): No need to project this variable with atomic, just try this class
    private AtomicInteger c = new AtomicInteger(0);

    @Override
    public void onReceive(Object o) throws Throwable {
        if (o instanceof Integer) {
            Integer i = (Integer) o;
            if (i.intValue() == -1) {
                c.decrementAndGet();
            } else {
                c.addAndGet(i.intValue());
            }
            logger.info("Current task count: " + c.get());
            if (c.get() == 0) {
                CrawlSystem.exportActor.tell(EXPORT, this.getSelf());
            }
        } else if (o instanceof String) {
            String str = (String) o;
            if (str.equals(SHUTDOWN)) {
                this.getSelf().tell(PoisonPill.getInstance(), getSelf());
                getContext().become(shuttingDown);
                AsyncHttpHandler.getInstance().close();
            } else if (str.equals(DONE) && this.getSender() == CrawlSystem.exportActor) {
                ActorRef mediator = DistributedPubSub.get(getContext().system()).mediator();
                mediator.tell(new DistributedPubSubMediator.Publish("terminator", SHUTDOWN),
                        getSelf());
            }
        } else
            unhandled(o);
    }

    @Override
    public void postStop() {
        // clean up resources here ...
        this.getContext().system().terminate();
    }
}
