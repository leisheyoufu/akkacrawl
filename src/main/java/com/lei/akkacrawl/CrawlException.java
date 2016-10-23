package com.lei.akkacrawl;

/**
 * Created by cl on 2016/10/16.
 */
public class CrawlException extends Exception{
    public CrawlException(String msg, Throwable t){
        super(msg, t);
    }
    public CrawlException(String msg) {
        super(msg);
    }
}
