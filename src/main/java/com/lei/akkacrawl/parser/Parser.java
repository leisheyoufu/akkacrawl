package com.lei.akkacrawl.parser;

import com.google.inject.Inject;
import com.lei.akkacrawl.CrawlException;
import com.lei.akkacrawl.TaskEntry;
import java.util.Set;

public class Parser {
    private Service service;
    @Inject
    public Parser(Service service) {
        this.service = service;
    }
    public Set<TaskEntry> parse(TaskEntry entry, String content, String contentType) throws CrawlException {
        return this.service.process(entry, content, contentType);
    }
}
