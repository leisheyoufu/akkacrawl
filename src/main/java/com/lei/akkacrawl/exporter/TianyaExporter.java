package com.lei.akkacrawl.exporter;

import com.lei.akkacrawl.Command;
import com.lei.akkacrawl.actor.CrawlSystem;
import com.lei.akkacrawl.parser.TianyaService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;


public class TianyaExporter extends Exporter{
    private String content;
    private String title;
    private List<TianyaService.TianyaContent> authorContentList = new ArrayList<>();
    private static Map<String, TianyaExporter> instances =new HashMap<>();

    public TianyaExporter(String title, String content) {
        if (this.content == null) {
            this.content = content;
        }
        this.title = title;
    }

    public void registe(List<TianyaService.TianyaContent> authorContentList) {
        this.authorContentList.addAll(authorContentList);
    }

    public static TianyaExporter getInstance(String title, String content) {
        if (!instances.containsKey(title)) {
            synchronized (TianyaExporter.class) {
                if (!instances.containsKey(title)) {
                    TianyaExporter instance = new TianyaExporter(title, content);
                    instances.put(title, instance);
                }
            }
        }
        return instances.get(title);
    }

    public void build() {
        List<String> atlItemList = new LinkedList<>();
        Document doc = Jsoup.parse(content);
        for (TianyaService.TianyaContent item : authorContentList) {
            Elements tempList = doc.select(".atl-item");
            if (tempList.size() < 2) {
                continue;
            }
            String atlItemStr = tempList.get(1).clone().toString();
            Document atlItem = Jsoup.parse(atlItemStr);
            // change content
            tempList = atlItem.select(".bbs-content");
            if (tempList.size() < 1) {
                continue;
            }
            tempList.get(0).html(item.getAuthorContent());
            // change time
            if (item.getTime() != null) {
                tempList = atlItem.select(".atl-info");
                if (tempList.size() < 1) {
                    continue;
                }
                tempList = tempList.get(0).select("span");
                if (tempList.size() < 2) {
                    continue;
                }
                tempList.get(1).html(item.getTime());
            }
            // change author
            tempList =  atlItem.select(".atl-info");
            if (tempList.size() < 1) {
                continue;
            }
            tempList = tempList.get(0).select("span");
            if (tempList.size() < 1) {
                continue;
            }
            tempList = tempList.get(0).select("a");
            if(tempList.size() < 1){
                continue;
            }
            tempList.get(0).html(item.getAuthor());
            atlItemList.add(atlItem.toString());
        }
        StringBuilder sb = new StringBuilder();
        for (String item : atlItemList) {
            sb.append(item);
        }
        doc.select(".atl-main").get(0).html(sb.toString());
        // remove page symbol
        Elements atlPageElements = doc.select(".atl-pages");
        for (Element atlpageElement : atlPageElements) {
            atlpageElement.html("");
        }

        Elements replayItemElements = doc.select(".ir-list");
        for (Element replayItem : replayItemElements) {
            replayItem.html("");
        }

        // remove reply comment
        Elements replyElements = doc.select(".item-reply-view");
        for (Element replyElement : replyElements) {
            replyElement.html();
        }
        buildHtml(doc);
    }

    private void buildHtml(Document doc) {
        try {
            if (CrawlSystem.settings.get("export") == null || CrawlSystem.settings.get("export").get("path") == null) {
                logger.error("Configuration errer, export path is not supplied.");
                System.exit(Command.ExitCodes.ERROR);
            }
            String exportPath = CrawlSystem.settings.get("export").get("path").toString();
            File dir = new File(exportPath);
            if (!dir.exists()) {
                dir.mkdir();
            }
            FileOutputStream out = new FileOutputStream(new File(exportPath, title + ".html"));
            out.write(doc.toString().getBytes());
            out.close();
            logger.info(exportPath + " generated");
        } catch (IOException e) {
            logger.error(e);
        }
    }
}
