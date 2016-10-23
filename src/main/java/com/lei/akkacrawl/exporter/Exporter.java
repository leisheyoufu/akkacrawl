package com.lei.akkacrawl.exporter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class Exporter {
    protected Logger logger = LogManager.getLogger(this.getClass().getName());
    abstract public void build();
}
