package pt.raidline.api.fuzzy.processors.schema;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public final class ValueRandomizer {

    private static final Supplier<ThreadLocalRandom> random = ThreadLocalRandom::current;
    private static final String[] alphabet = new String[] {
            "A"
    };

    private ValueRandomizer() {
    }

    //todo: complete
    public static String randomizeStringValue(StringFormat format) {
        var randomizer = random.get();
        return switch (format) {
            case DATE_TIME -> LocalDateTime.now()
                    .plusYears(randomizer.nextLong(10))
                    .plusDays(randomizer.nextLong(50))
                    .toString();
            case DATE -> Date.from(Instant.now())
                    .toString();
            case DEFAULT -> "\"some string\"";
        };
    }

    public static int randomizeIntValue() {
        return 1;
    }

    public static boolean randomizeBoolValue() {
        return false;
    }

    public enum StringFormat {
        DATE_TIME("date-time"), DATE("date"), DEFAULT("default");

        final String format;

        StringFormat(String format) {
            this.format = format;
        }

        public static StringFormat fromString(String form) {
            if (form == null) {
                return DEFAULT;
            }
            return switch (form) {
                case "date-time" -> DATE_TIME;
                case "date" -> DATE;
                default -> DEFAULT;
            };
        }
    }
}
