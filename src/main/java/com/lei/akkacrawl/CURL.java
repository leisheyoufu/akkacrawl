package com.lei.akkacrawl;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class CURL implements Comparable<CURL> {
    private URL url;
    private URI uri;
    private String origUrl;
    private String referer;
    private int depth = 0;

    public URL getUrl() {
        return url;
    }

    public URI getUri() {
        return uri;
    }

    public CURL(String url) throws MalformedURLException, URISyntaxException {
        this.origUrl = url;
        this.url = new URL(url);
        this.uri = new URI(this.url.getProtocol(), this.url.getHost(), this.url.getPath(),
                this.url.getQuery(), null);
    }

    public CURL(String url, int depth) throws MalformedURLException, URISyntaxException {
        this(url);
        this.depth = depth;
    }

    public String getReferer() {
        return referer;
    }

    public void setReferer(String referer) {
        this.referer = referer;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    @Override
    public int hashCode() {
        return this.origUrl.hashCode();
    }

    public String toString() {
        return this.origUrl;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof CURL) {
            CURL temp = (CURL) object;
            return this.origUrl.equals(temp.origUrl);
        }
        return false;
    }

    @Override
    public int compareTo(CURL o) {
        return this.origUrl.compareTo(o.origUrl);
    }
}
