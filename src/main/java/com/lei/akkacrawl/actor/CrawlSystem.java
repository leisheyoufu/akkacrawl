package com.lei.akkacrawl.actor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Inbox;
import akka.actor.Props;
import com.lei.akkacrawl.CURL;
import com.lei.akkacrawl.CrawlException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class CrawlSystem {
    public static ActorSystem system = ActorSystem.create("CrawlSystem");
    public static ActorRef crawlActor = system.actorOf(Props.create(CrawlActor.class), "httpreqeust");
    public static ActorRef downloadActor = system.actorOf(Props.create(DownloadActor.class), "httpdownloader");
    public static ActorRef terminateActor = system.actorOf(Props.create(TerminateActor.class), "terminator");
    public static ActorRef exportActor = system.actorOf(Props.create(ExporteActor.class), "exporter");
    public static Map<String, HashMap> settings;

    public static void start(Map<String, HashMap> settings) throws CrawlException {
        CURL curl;
        try {
            curl = new CURL(settings.get("site").get("url").toString(),
                    Integer.parseInt(settings.get("site").get("template").toString()));
        } catch (MalformedURLException | URISyntaxException e) {
            throw new CrawlException("Incorrect url: " + settings.get("site").get("url"), e);
        }
        CrawlSystem.settings = settings;
        Inbox inbox = Inbox.create(system);
        inbox.send(crawlActor, curl);
        inbox.send(terminateActor, 1);
    }
}
