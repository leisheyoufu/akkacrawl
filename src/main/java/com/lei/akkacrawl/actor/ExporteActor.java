package com.lei.akkacrawl.actor;

import akka.actor.PoisonPill;
import akka.cluster.pubsub.DistributedPubSubMediator;
import com.lei.akkacrawl.exporter.Exporter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class ExporteActor extends BaseActor {
    private List<Exporter> exporters = new ArrayList<>();
    @Override
    public void onReceive(Object o) throws Throwable {
        if (o instanceof DistributedPubSubMediator.SubscribeAck) {
            logger.info(this.getClass().getName()+": Subscribe ack");
        } else if (o instanceof Exporter) {
            if (!exporters.contains(o)) {
                exporters.add((Exporter) o);
            }
        } else if (o instanceof String) {
            String str = (String) o;
            if (str.equals(SHUTDOWN)) {
                this.getSelf().tell(PoisonPill.getInstance(), getSelf());
                getContext().become(shuttingDown);
            } else if(str.equals(EXPORT)) {
                for(Exporter e: exporters) {
                    e.build();
                }
                CrawlSystem.terminateActor.tell(DONE, this.getSelf());
            }
        } else
            unhandled(o);
    }



}
