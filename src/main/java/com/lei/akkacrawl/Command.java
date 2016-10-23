package com.lei.akkacrawl;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public abstract class Command {
    protected final OptionParser parser = new OptionParser();
    private final OptionSpec<Void> helpOption = parser.acceptsAll(Arrays.asList("h", "help"), "show help").forHelp();

    private final OptionSpec<String> configOptions = parser.acceptsAll(
            Arrays.asList("c", "config"), "configuration file").withRequiredArg().ofType(String.class);

    /**
     * Parses options for this command from args and executes it.
     */
    public final int main(final Command command, String[] args) {
        try {
            return mainWithoutErrorHandling(command, args);
        } catch (OptionException e) {
            printHelp();
            return ExitCodes.USAGE;
        } catch (Exception e) {
            e.printStackTrace();
            return ExitCodes.ERROR;
        }
    }

    /**
     * Prints a help message for the command to the terminal.
     */
    private void printHelp() {
        try {
            parser.printHelpOn(System.out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class ExitCodes {
        public static final int OK = 0;
        public static final int USAGE = 64;          /* command line usage error */
        public static final int ERROR = 127;
        public static final int ARGS_ERROR = 65;

        private ExitCodes() { /* no instance, just constants */ }
    }

    int mainWithoutErrorHandling(final Command command, String[] args) throws Exception {
        final OptionSet options = parser.parse(args);
        File configFile = new File("config", "akkacrawl.json");
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream config = classloader.getResourceAsStream(configFile.getPath());
        if (options.has(helpOption)) {
            printHelp();
            return ExitCodes.USAGE;
        }
        if (options.has(configOptions)) {
            configFile = new File((String) options.valueOf("c"));
            config = new FileInputStream(configFile.getAbsolutePath());
            if (!configFile.getAbsoluteFile().exists() || !configFile.canRead()) {
                return ExitCodes.ARGS_ERROR;
            }
        }
        return execute(command, config);
    }

    protected abstract int execute(final Command command, InputStream config) throws Exception;
}
