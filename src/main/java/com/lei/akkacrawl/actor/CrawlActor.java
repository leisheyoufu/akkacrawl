package com.lei.akkacrawl.actor;

import akka.actor.PoisonPill;
import akka.cluster.pubsub.DistributedPubSubMediator;
import com.lei.akkacrawl.AsyncHttpHandler;
import com.lei.akkacrawl.CURL;
import com.lei.akkacrawl.TaskEntry;

public class CrawlActor extends BaseActor {
    @Override
    public void onReceive(Object o) throws Throwable {
        AsyncHttpHandler handler = AsyncHttpHandler.getInstance();
        if (o instanceof CURL) {
            CURL curl = (CURL) o;
            TaskEntry entry = new TaskEntry(curl);
            handler.process(entry);
        } else if (o instanceof TaskEntry) {
            TaskEntry entry = (TaskEntry) o;
            handler.process(entry);
        } else if (o instanceof DistributedPubSubMediator.SubscribeAck) {
            logger.info(this.getClass().getName()+": Subscribe ack");
        } else if (o instanceof String) {
            String str = (String) o;
            if (str.equals(SHUTDOWN)) {
                this.getSelf().tell(PoisonPill.getInstance(), getSelf());
                getContext().become(shuttingDown);
            }
        } else
            unhandled(o);
    }



}
