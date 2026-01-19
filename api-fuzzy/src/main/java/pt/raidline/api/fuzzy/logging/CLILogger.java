package pt.raidline.api.fuzzy.logging;

import java.util.Objects;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class CLILogger {

    public static boolean DEBUG_MODE = true;

    private static final Logger LOGGER = Logger.getGlobal();

    // ANSI color codes
    private static final String RESET = "\u001B[0m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String BLUE = "\u001B[34m";

    static {
        LOGGER.setLevel(Level.ALL);

        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        handler.setFormatter(new ColoredFormatter());
        LOGGER.addHandler(handler);
        LOGGER.setUseParentHandlers(false);
    }

    private CLILogger() {
    }

    public static void setDebugMode(boolean debugMode) {
        DEBUG_MODE = debugMode;
    }

    public static void severe(String message, Object... params) {
        var caller = getCallerInfo();
        LOGGER.log(Level.SEVERE, () ->
                formatStringForPrinting(caller.className, caller.lineNumber, String.format(message, params)));
    }

    public static void info(String message, Object... params) {
        var caller = getCallerInfo();
        LOGGER.log(Level.INFO, () -> formatStringForPrinting(caller.className, caller.lineNumber, String.format(message, params)));
    }

    public static void debug(String message, Object... params) {
        if (DEBUG_MODE) {
            var caller = getCallerInfo();
            LOGGER.log(Level.CONFIG, () -> formatStringForPrinting(caller.className, caller.lineNumber, String.format(message, params)));
        }
    }

    public static void warn(String message, Object... params) {
        var caller = getCallerInfo();
        LOGGER.log(Level.WARNING, () -> formatStringForPrinting(caller.className, caller.lineNumber, String.format(message, params)));
    }

    private static String formatStringForPrinting(Object... params) {
        var className = (String) params[0];
        var lineNumber = (int) params[1];
        var message = (String) params[2];

        return "[%s]-[%s:%d] %s".formatted(Thread.currentThread().getName(), className, lineNumber, message);
    }

    private static CallerInfo getCallerInfo() {
        return StackWalker.getInstance()
                .walk(frames -> frames
                        .filter(f -> !f.getClassName().equals(CLILogger.class.getName()))
                        .findFirst()
                        .map(f -> new CallerInfo(f.getClassName().substring(f.getClassName().lastIndexOf('.') + 1), f.getLineNumber()))
                        .orElse(new CallerInfo("Unknown", 0)));
    }

    private record CallerInfo(String className, int lineNumber) {
    }

    private static class ColoredFormatter extends Formatter {
        @Override
        public String format(LogRecord logRecord) {
            String color = switch (logRecord.getLevel().getName()) {
                case "SEVERE" -> RED;
                case "INFO" -> BLUE;
                case "WARNING" -> YELLOW;
                case "CONFIG" -> GREEN;
                default -> RESET;
            };

            return String.format("%s[%s] %s%s%n",
                    color,
                    logRecord.getLevel().getName(),
                    logRecord.getMessage(),
                    RESET);
        }
    }
}
