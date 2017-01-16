package com.lei.akkacrawl;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.lei.akkacrawl.actor.CrawlSystem;
import com.lei.akkacrawl.parser.ParserModule;
import com.lei.akkacrawl.parser.Parser;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.CodingErrorAction;
import java.util.*;

import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mozilla.universalchardet.UniversalDetector;

public class AsyncHttpHandler {
    protected Logger logger = LogManager.getLogger(this.getClass().getName());
    private AsyncHttpHandler() {
        this.httpClient = buildClient();
        this.httpClient.start();
    }

    public void close() throws IOException {
        this.httpClient.close();
    }

    private CloseableHttpAsyncClient buildClient() {
        HttpAsyncClientBuilder asyncClientBuilder = HttpAsyncClientBuilder.create();

        // reactor config
        IOReactorConfig reactorConfig = IOReactorConfig.custom()
                .setConnectTimeout(TIMEOUT_2_MINS_IN_MILLIS)
                .setSoTimeout(TIMEOUT_2_MINS_IN_MILLIS).build();

        asyncClientBuilder.setDefaultIOReactorConfig(reactorConfig);

        // request config
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(TIMEOUT_2_MINS_IN_MILLIS)
                .setConnectionRequestTimeout(TIMEOUT_2_MINS_IN_MILLIS)
                .setSocketTimeout(TIMEOUT_2_MINS_IN_MILLIS).build();
        asyncClientBuilder.setDefaultRequestConfig(requestConfig);

        // connection config
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setMalformedInputAction(CodingErrorAction.IGNORE)
                .setUnmappableInputAction(CodingErrorAction.IGNORE)
                .build();
        asyncClientBuilder.setDefaultConnectionConfig(connectionConfig);
        asyncClientBuilder.disableCookieManagement();

        System.setProperty("http.maxConnections", "100");
        System.setProperty("http.conn-manager.timeout", String.valueOf(TIMEOUT_2_MINS_IN_MILLIS)); // 2 mins

        return asyncClientBuilder.useSystemProperties().build();
    }

    private static AsyncHttpHandler instance = null;

    public static AsyncHttpHandler getInstance() {
        if (instance == null) {
            synchronized (AsyncHttpHandler.class) {
                if (instance == null) {
                    instance = new AsyncHttpHandler();
                }
            }
        }
        return instance;
    }

    private Map<FutureCallback, TaskEntry> taskMap = new HashMap<>();
    private final int TIMEOUT_2_MINS_IN_MILLIS = 2 * 60 * 1000;
    private CloseableHttpAsyncClient httpClient;

    public void process(TaskEntry entry) {
        final HttpGet request = new HttpGet(entry.getCurl().getUri());
        HttpFutureCallback callback = new HttpGETFutureCallback<>(this.taskMap);
        this.taskMap.put(callback, entry);
        httpClient.execute(request, callback);
    }

    public void download(TaskEntry entry) {
        final HttpGet request = new HttpGet(entry.getCurl().getUri());
        HttpFutureCallback callback = new HttpDownloadFutureCallback<>(this.taskMap);
        this.taskMap.put(callback, entry);
        httpClient.execute(request, callback);
    }

    private abstract class HttpFutureCallback<T extends HttpResponse> implements FutureCallback<T> {
        private Map<FutureCallback, TaskEntry> taskMap;

        public HttpFutureCallback(Map taskMap) {
            this.taskMap = taskMap;
        }

        @Override
        public void failed(Exception ex) {
            TaskEntry entry = taskMap.get(this);
            logger.error("Failed retry time " + entry.getRetry() + ": " + entry.getCurl().toString());
            entry.setRetry(entry.getRetry()+1);
            if (entry.getRetry() < 5) {
                if (entry.getType() == TaskEntry.TaskType.DOWNLOAD) {
                    CrawlSystem.downloadActor.tell(entry, null);
                } else if(entry.getType() == TaskEntry.TaskType.GET) {
                    CrawlSystem.crawlActor.tell(entry, null);
                }
            } else {
                CrawlSystem.terminateActor.tell(-1, null);
            }
        }

        @Override
        public void cancelled() {
            TaskEntry entry = taskMap.get(this);
            logger.info("Canceled: " + entry.getCurl().toString());
            entry.setState(TaskEntry.State.ERROR);
            CrawlSystem.terminateActor.tell(-1, null);
        }
    }
    private class HttpGETFutureCallback<T extends HttpResponse> extends HttpFutureCallback<T>
            implements FutureCallback<T> {
        public HttpGETFutureCallback(Map taskMap) {
            super(taskMap);
        }
        @Override
        public void completed(T result) {
            TaskEntry entry = taskMap.get(this);
            entry.setState(TaskEntry.State.RUNNING);
            // domain --> bind service to Target service, this mapping is constructed only once as it is initialized in the static scope
            // parser.process --> service.process
            Injector injector = Guice.createInjector(new ParserModule(entry.getCurl().getUri().getHost()));
            Parser parser = injector.getInstance(Parser.class);
            Set<TaskEntry> taskSet;
            try {
                UniversalDetector detector = new UniversalDetector(null);
                byte []buf = IOUtils.toByteArray(result.getEntity().getContent());
                detector.handleData(buf,0,buf.length);
                detector.dataEnd();
                String encoding = detector.getDetectedCharset();
                detector.reset();
                String contentType = result.getHeaders("Content-Type")[0].getValue();
                // buf to string with the target charset encoding
                taskSet = parser.parse(entry,IOUtils.toString(buf, encoding), contentType);
                for (TaskEntry t:taskSet) {
                    if (t.getType() == TaskEntry.TaskType.DOWNLOAD) {
                        CrawlSystem.downloadActor.tell(t, null);
                    } else if(t.getType() == TaskEntry.TaskType.GET) {
                        CrawlSystem.crawlActor.tell(t, null);
                    }
                }
                CrawlSystem.terminateActor.tell(taskSet.size(),null);
            } catch (CrawlException | IOException e) {
                e.printStackTrace();
            }
            CrawlSystem.terminateActor.tell(-1,null);
        }
    }

    private class HttpDownloadFutureCallback<T extends HttpResponse> extends HttpFutureCallback<T>
            implements FutureCallback<T> {
        public HttpDownloadFutureCallback(Map taskMap) {
            super(taskMap);
        }
        @Override
        public void completed(T result) {
            TaskEntry entry = taskMap.get(this);
            try {
                File dir = new File("s:/pic");
                if (!dir.exists()) {
                    dir.mkdir();
                }
                FileOutputStream out = new FileOutputStream(new File(entry.getDownloadDest()));
                result.getEntity().writeTo(out);
                out.close();
                entry.setState(TaskEntry.State.DONE);
            } catch (IOException e) {
                e.printStackTrace();
            }
            CrawlSystem.terminateActor.tell(-1,null);
        }
    }
}
