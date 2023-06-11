package pt.ulisboa.tecnico.cnv.middleware;

import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class CustomLogger {

    private static Logger LOGGER;

    public CustomLogger(String name) {
        LOGGER = Logger.getLogger(name);
        LOGGER.setLevel(Level.ALL);
        LOGGER.setUseParentHandlers(false);
        ConsoleHandler handler = new ConsoleHandler();

        Formatter formatter = new CustomLog();
        handler.setFormatter(formatter);

        LOGGER.addHandler(handler);
    }

    public void log(Level level, String message) {
        LOGGER.log(level, message);
    }

    public void log(String message) {
        LOGGER.log(Level.INFO, message);
    }
}

class CustomLog extends Formatter {
    @Override
    public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();
        sb.append(record.getMessage()).append('\n');
        return sb.toString();
    }
}