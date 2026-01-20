package pt.raidline.api.fuzzy.assertions;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BooleanSupplier;

import pt.raidline.api.fuzzy.logging.CLILogger;

public final class AssertionUtils {

    public static boolean DUMP_OUTSIDE_FRAMES = false;
    private static final Map<String, String[]> PRECONDITION_KEY_TO_ERRORS = new HashMap<>();

    private static final String DEBUG_FILE_NAME =
            "api_fuzzy_file_debug_" +
                    ThreadLocalRandom.current().nextLong() +
                    ".txt";
    private static final File DEBUG_FILE = new File(
            System.getProperty("user.dir"),
            DEBUG_FILE_NAME
    );
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final String SEPARATOR = "═".repeat(80);
    private static final String THIN_SEPARATOR = "─".repeat(80);

    private AssertionUtils() {
    }

    public static void precondition(
            String key,
            String message,
            BooleanSupplier precondition
    ) {
        if (!precondition.getAsBoolean()) {
            CLILogger.warn(
                    "Precondition [%s] failed. Reason: [%s]",
                    key,
                    message
            );

            System.exit(1);
        }
    }

    public static void internalAssertion(String key, BooleanSupplier action) {
        internalAssertion(key, action, "Some error has occurred");
    }

    public static void internalAssertion(String key, BooleanSupplier action, String message) {
        if (!action.getAsBoolean()) {
            String logMessage = String.format(
                    "There has been an error for the operation: %s : %s",
                    key,
                    message
            );
            CLILogger.severe(
                    "There has been an error for the operation: %s : %s",
                    key,
                    message
            );

            writeToFile("ASSERTION FAILURE", logMessage);
            logStacktrace();

            System.exit(1);
        }
    }

    public static ErrorsAggregator aggregateErrors(String key) {
        return new ErrorAggregation(key);
    }

    private static void writeToFile(String logType, String logMessage) {
        try {
            if (!DEBUG_FILE.exists()) {
                DEBUG_FILE.createNewFile();
            }

            try (PrintWriter writer = new PrintWriter(new FileWriter(DEBUG_FILE, true))) {
                String timestamp = LocalDateTime.now().format(DATE_FORMATTER);

                writer.println();
                writer.println(SEPARATOR);
                writer.printf("║ [%s] %s%n", logType, timestamp);
                writer.println(THIN_SEPARATOR);
                writer.println("║ MESSAGE:");
                for (String line : logMessage.split("\n")) {
                    writer.printf("║   %s%n", line);
                }
                writer.println(THIN_SEPARATOR);
                writer.println("║ STACKTRACE:");

                var frames = StackWalker.getInstance().walk(frame -> {
                    if (!DUMP_OUTSIDE_FRAMES) {
                        return frame
                                .filter(s ->
                                        s
                                                .getClassName()
                                                .contains("pt.raidline.api.fuzzy")
                                )
                                .toList();
                    }
                    return frame.toList();
                });

                for (var frame : frames) {
                    writer.printf(
                            "║   → %s#%s (Line %d)%n",
                            frame.getClassName(),
                            frame.getMethodName(),
                            frame.getLineNumber()
                    );
                }

                writer.println(SEPARATOR);
                writer.flush();
            }
        } catch (IOException e) {
            CLILogger.severe(
                    "Could not write to DEBUG File: %s",
                    e.getMessage()
            );
        }
    }

    private static void logStacktrace() {
        var frames = StackWalker.getInstance().walk(frame -> {
            if (!DUMP_OUTSIDE_FRAMES) {
                return frame
                        .filter(s ->
                                s.getClassName().contains("pt.raidline.api.fuzzy")
                        )
                        .toList();
            }

            return frame.toList();
        });

        CLILogger.info("Stacktrace");

        frames
                .stream()
                .map(
                        f ->
                                f.getClassName() +
                                        "#" +
                                        f.getMethodName() +
                                        ", Line " +
                                        f.getLineNumber()
                )
                .forEach(f -> CLILogger.severe("%s", f));
    }

    public interface ErrorsAggregator {
        ErrorsAggregator onError(String message, BooleanSupplier condition);

        void complete();
    }

    private static class ErrorAggregation implements ErrorsAggregator {
        private final List<String> errors = new ArrayList<>(4); // 4 as the best effort
        private final String key;

        private ErrorAggregation(String key) {
            this.key = key;
        }

        @Override
        public ErrorsAggregator onError(String message, BooleanSupplier condition) {
            Objects.requireNonNull(message);
            Objects.requireNonNull(condition);

            if (!condition.getAsBoolean()) {
                errors.add("Precondition [%s] failed. Reason: [%s]"
                        .formatted(key, message));
            }

            return this;
        }

        @Override
        public void complete() {
            for (String error : this.errors) {
                CLILogger.warn(
                        "Precondition [%s] failed. Reason: [%s]",
                        key,
                        error
                );
            }
        }
    }
}
