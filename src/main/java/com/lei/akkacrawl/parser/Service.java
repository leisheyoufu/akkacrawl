package com.lei.akkacrawl.parser;

import com.lei.akkacrawl.CrawlException;
import com.lei.akkacrawl.TaskEntry;

import java.util.Set;

public interface Service {
    public Set<TaskEntry> process(TaskEntry entry, String content, String contentType) throws CrawlException;
}
