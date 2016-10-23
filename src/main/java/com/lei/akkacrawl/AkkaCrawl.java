package com.lei.akkacrawl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lei.akkacrawl.actor.CrawlSystem;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class AkkaCrawl extends Command {

    private Map<String, HashMap> processSetting(InputStream config) throws IOException {
        byte[] mapData = IOUtils.toByteArray(config);
        Map<String, HashMap> settings = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        settings = objectMapper.readValue(mapData, HashMap.class);
        return settings;
    }

    public static void main(String args[]) {
        final Command crawl = new AkkaCrawl();
        int status = crawl.main(crawl, args);
        if (status != ExitCodes.OK) {
            System.exit(status);
        }
    }

    @Override
    protected int execute(final Command command, InputStream config) throws Exception {
        Map<String, HashMap> settings;
        try {
            settings = processSetting(config);
            CrawlSystem.start(settings);
        } catch (IOException | CrawlException e) {
            e.printStackTrace();
            return ExitCodes.ERROR;
        }
        return ExitCodes.OK;
    }
}
