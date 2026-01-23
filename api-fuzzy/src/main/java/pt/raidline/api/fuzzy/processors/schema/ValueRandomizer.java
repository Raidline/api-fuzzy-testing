package pt.raidline.api.fuzzy.processors.schema;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public final class ValueRandomizer {

    private static final Supplier<ThreadLocalRandom> random = ThreadLocalRandom::current;
    private static final String[] alphabet = new String[]{
            "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m",
            "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"
    };

    private static final String[] alphabetUpperCase = new String[]{
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M",
            "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"
    };

    private static final String[] symbols = new String[]{
            "!", "\"", "#", "$", "%", "&", "'", "(", ")", "*", "+", ",", "-", ".", "/",
            ":", ";", "<", "=", ">", "?", "@", "[", "\\", "]", "^", "_", "`", "{", "|", "}", "~"
    };

    private ValueRandomizer() {
    }

    public static String randomizeStringValue(StringFormat format) {
        var randomizer = random.get();
        return switch (format) {
            case DATE_TIME -> LocalDateTime.now()
                    .plusYears(randomizer.nextLong(10))
                    .plusDays(randomizer.nextLong(50))
                    .toString();
            case DATE -> Date.from(Instant.now())
                    .toString();
            case DEFAULT -> "\"" + randomString(randomizer) + "\"";
        };
    }

    private static String randomString(ThreadLocalRandom randomizer) {
        var stringLen = randomizer.nextInt(0, 15);
        var output = new StringBuilder();
        for (int i = 0; i < stringLen; i++) {
            var alphabetRndIdx = randomizer.nextInt(0, alphabet.length);
            var alphabetUpperRndIdx = randomizer.nextInt(0, alphabetUpperCase.length);
            var symbolsRndIdx = randomizer.nextInt(0, symbols.length);

            if (randomizeBoolValue()) { // Allow Upper case
                output.append(alphabetUpperCase[alphabetUpperRndIdx]);
            } else {
                output.append(alphabet[alphabetRndIdx]);
            }

            if (randomizeBoolValue()) { // Allow symbols
                output.append(symbols[symbolsRndIdx]);
            }
        }

        return output.toString();
    }

    public static int randomizeIntValue() {
        var randomizer = random.get();
        return randomizer.nextInt();
    }

    public static boolean randomizeBoolValue() {
        var randomizer = random.get();
        return randomizer.nextBoolean();
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
