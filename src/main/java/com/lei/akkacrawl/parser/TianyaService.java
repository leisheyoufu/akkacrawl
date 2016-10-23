package com.lei.akkacrawl.parser;

import com.google.inject.Singleton;
import com.lei.akkacrawl.CURL;
import com.lei.akkacrawl.CrawlException;
import com.lei.akkacrawl.TaskEntry;
import com.lei.akkacrawl.actor.CrawlSystem;
import com.lei.akkacrawl.exporter.TianyaExporter;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.Segment;
import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.StartTag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Singleton
public class TianyaService implements Service {
    static Logger logger = LogManager.getLogger(TianyaService.class.getName());
    public static final String domain = "bbs.tianya.cn";
    private String user = CrawlSystem.settings.get("site").get("user").toString();
    private int delta = Integer.parseInt(CrawlSystem.settings.get("site").get("delta").toString());

    private String joinUrl(String nextPage) {
        StringBuilder sb = new StringBuilder();
        sb.append("http://");
        sb.append(domain);
        sb.append(nextPage);
        return new String(sb);
    }

    public Set<TaskEntry> process(TaskEntry entry, String content, String contentType) throws CrawlException {

        Set<TaskEntry> taskSet = new HashSet<>();
        if (entry.getCurl().getDepth() == 0) {
            processNextIndex(entry, content, contentType, taskSet);
            processIndexByAuthor(content, user, taskSet);
        } else if (entry.getCurl().getDepth() == 1) {
            String title = getTitle(content);
            processNextContentPage(entry, content, contentType, taskSet);
            List<TianyaContent> tianyaContentList = processContentPageByAuthor(entry, content, contentType, taskSet, user);
            TianyaExporter exporter = TianyaExporter.getInstance(title, content);
            exporter.registe(tianyaContentList);
            CrawlSystem.exportActor.tell(exporter, null);
        }
        return taskSet;
    }

    private  List<TianyaContent> processContentPageByAuthor(TaskEntry entry, String content, String contentType,
                                            Set<TaskEntry> taskSet, String author) {
        List<TianyaContent> contentList = new ArrayList<>();
        Source source = new Source(content);
        String hostContent;
        TianyaContent tianyaContent;
        Element altMain = source.getFirstElementByClass("atl-main");

        if (altMain != null) {
            if (altMain.getFirstElementByClass("bbs-content clearfix") != null) {
                hostContent = altMain.getFirstElementByClass("bbs-content clearfix").getContent().toString();
                tianyaContent = new TianyaContent(this.user, hostContent);
                contentList.add(tianyaContent);
                logger.debug(hostContent);
            }
        }
        List<Element> altInfoList = source.getAllElementsByClass("atl-item");
        for( Element altInfo : altInfoList) {
            Element temp;
            if (altInfo.getAttributeValue("_host").equals(author)) {
                temp = altInfo.getFirstElementByClass("bbs-content");
                if (temp == null) {
                    continue;
                }
                hostContent =temp.getContent().toString();
                temp =  altInfo.getFirstElementByClass("atl-info");
                if (temp == null) {
                    continue;
                }
                if (temp.getAllStartTags("span").size() > 1) {
                    temp = temp.getAllStartTags("span").get(1).getElement();
                    String time = temp.getContent().toString();
                    tianyaContent = new TianyaContent(this.user, hostContent, time);
                    contentList.add(tianyaContent);
                }
            }
        }
        return contentList;
    }

    private void processNextContentPage(TaskEntry entry, String content, String contentType,
                                        Set<TaskEntry> taskSet) {
        Source source = new Source(content);
        Element altPages = source.getFirstElementByClass("atl-pages");
        if (null == altPages) {
            return;
        }
        List<StartTag> altPageTags = altPages.getAllStartTags("a");
        for(StartTag altPageTag: altPageTags) {
            if (altPageTag.getElement().getContent().toString().equals("下页")) {
                String url = altPageTag.getElement().getAttributeValue("href").toString();
                url =  joinUrl(url);
                CURL curl;
                try {
                    curl = new CURL(url, 1);
                } catch (MalformedURLException | URISyntaxException e) {
                    logger.error(e);
                    return;
                }
                TaskEntry newEntry = new TaskEntry(curl, TaskEntry.TaskType.GET, TaskEntry.State.ENROLL, 0);
                logger.info("Add " + newEntry.getCurl().toString());
                taskSet.add(newEntry);
                return;
            }
        }

    }

    private void processNextIndex(TaskEntry entry, String content, String contentType, Set<TaskEntry> taskSet) {
        Source source = new Source(content);
        boolean in = inDataRange(source);
        entry.setState(TaskEntry.State.DONE);
        if (!in) {
            return;
        }
        Element linksElement = source.getFirstElementByClass("links");
        List<StartTag> aTagList = linksElement.getContent().getAllStartTags("a");
        String nextPageLink, url;
        for (StartTag aTag : aTagList) {
            String tagContent = aTag.getElement().getContent().toString();
            if (contentType.contains("ISO-8859-1")) {
                try {
                    tagContent = new String(tagContent.getBytes("ISO-8859-1"), "UTF8");
                } catch (UnsupportedEncodingException e) {
                    logger.error(e);
                }
            }
            if (tagContent.equals("下一页")) {
                nextPageLink = aTag.getElement().getAttributeValue("href");
                url = joinUrl(nextPageLink);
                CURL curl;
                try {
                    curl = new CURL(url, 0);
                } catch (MalformedURLException | URISyntaxException e) {
                    logger.error(e);
                    continue;
                }
                if (!curl.equals(entry.getCurl())) {
                    TaskEntry newEntry = new TaskEntry(curl, TaskEntry.TaskType.GET, TaskEntry.State.ENROLL, 0);
                    logger.info("Add " + newEntry.getCurl().toString());
                    taskSet.add(newEntry);
                }
            }
        }
    }

    private boolean inDataRange(Source source) {
        List<StartTag> tdList = source.getAllStartTags("td");
        for (StartTag tdItem : tdList) {
            String date = tdItem.getElement().getAttributeValue("title");
            if (date != null && date.length() > 0) {
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                Date now = new Date();
                Date pageDate = null;
                try {
                    pageDate = df.parse(date);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                long delta = now.getTime() - pageDate.getTime();
                long deltaInDay = delta / (24 * 60 * 60 * 1000);
                if (deltaInDay < this.delta) {
                    return true;
                }
            }
        }
        return false;
    }

    private void processIndexByAuthor(String content, String focus, Set<TaskEntry> taskSet) {
        Source source = new Source(content);
        List<StartTag> trList = source.getFirstElementByClass("tab-bbs-list tab-bbs-list-2").getContent().getAllStartTags("tr");
        for (StartTag tr : trList) {
            List<StartTag> tdList = tr.getElement().getAllStartTags("td");
            boolean store = false;
            String pageUrl = null;
            for (StartTag td : tdList) {
                StartTag aTag = td.getElement().getFirstStartTag("a");
                if(aTag == null) {
                    continue;
                }
                List<Element> authorElementList = aTag.getElement().getAllElementsByClass("author");
                if (authorElementList.size() > 0) {
                    if (authorElementList.get(0).getContent().toString().equals(focus)) {
                        logger.info(authorElementList.get(0).getContent().toString());
                        store = true;
                    }

                } else {
                    pageUrl = aTag.getElement().getAttributeValue("href");
                }
                if (store && pageUrl != null) {
                    String url = joinUrl(pageUrl);
                    CURL curl;
                    try {
                        curl = new CURL(url, 1);
                    } catch (MalformedURLException |URISyntaxException e) {
                        logger.error(e);
                        continue;
                    }
                    TaskEntry newEntry = new TaskEntry(curl, TaskEntry.TaskType.GET, TaskEntry.State.ENROLL, 0);
                    logger.info("Add " + newEntry.getCurl().toString());
                    taskSet.add(newEntry);
                }
            }
        }
    }

    private String getTitle(String content) {
        Source source = new Source(content);
        if (source.getFirstElementByClass("s_title") != null) {
            Element element = source.getFirstElementByClass("s_title").getContent().getFirstElement("span");
            if (element != null) {
                return element.getContent().toString();
            }
        }
        return null;
    }

    public class TianyaContent {
        public TianyaContent(String author, String authorContent, String time) {
            this(author, authorContent);
            this.time = time;
        }
        public TianyaContent(String author, String authorContent) {
            this.author = author;
            this.authorContent = authorContent;
        }
        private String author;

        public String getAuthor() {
            return author;
        }

        public String getAuthorContent() {
            return authorContent;
        }

        public String getTime() {
            return time;
        }

        private String authorContent;
        private String time;
    }
}
