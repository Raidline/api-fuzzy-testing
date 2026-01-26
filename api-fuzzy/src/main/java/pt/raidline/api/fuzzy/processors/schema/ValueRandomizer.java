package pt.raidline.api.fuzzy.processors.schema;

import com.mifmif.common.regex.Generex;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;
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

    private static final String[] DOMAINS = {"gmail", "yahoo", "hotmail", "outlook", "example"};
    private static final String[] TLDS = {".com", ".net", ".org", ".io", ".co"};

    private ValueRandomizer() {
    }

    public static String randomizeStringValue(StringFormat format,
                                              List<String> enumValues,
                                              Integer lowerBound,
                                              Integer upperBound, String pattern) {
        var randomizer = random.get();
        return switch (format) {
            case DATE_TIME -> LocalDateTime.now()
                    .plusYears(randomizer.nextLong(10))
                    .plusDays(randomizer.nextLong(50))
                    .toString();
            case DATE -> Date.from(Instant.now())
                    .toString();
            case UUID -> UUID.randomUUID().toString();
            case EMAIL -> generateEmail();
            case URI -> randomString(randomizer, enumValues,
                    lowerBound, upperBound, pattern);
            case DEFAULT -> "\"" + randomString(randomizer, enumValues,
                    lowerBound, upperBound, pattern) + "\"";
        };
    }

    private static String generateEmail() {
        ThreadLocalRandom randomizer = random.get();
        var username = randomString(randomizer,
                null, 5, 10, null);

        String domain = DOMAINS[randomizer.nextInt(DOMAINS.length)];

        String tld = TLDS[randomizer.nextInt(TLDS.length)];

        return username + "@" + domain + tld;
    }

    private static String randomString(ThreadLocalRandom randomizer,
                                       List<String> enumValues,
                                       Integer lowerBound,
                                       Integer upperBound, String pattern) {

        RndBounds bounds = getRndBounds(lowerBound, upperBound);

        if (pattern != null) {
            return new Generex(pattern).random(bounds.lower(), bounds.upper());
        }

        if (enumValues != null && !enumValues.isEmpty()) {
            var rndIdx = randomizer.nextInt(0, enumValues.size());
            return enumValues.get(rndIdx);
        }

        var stringLen = randomizer.nextInt(bounds.lower(), bounds.upper());
        var output = new StringBuilder();
        for (int i = 0; i < stringLen; i++) {
            var alphabetRndIdx = randomizer.nextInt(0, alphabet.length);
            var alphabetUpperRndIdx = randomizer.nextInt(0, alphabetUpperCase.length);

            if (randomizeBoolValue()) { // Allow Upper case
                output.append(alphabetUpperCase[alphabetUpperRndIdx]);
            } else {
                output.append(alphabet[alphabetRndIdx]);
            }
        }

        return output.toString();
    }

    public static int randomizeIntValue(Integer lowerBound, Integer upperBound) {
        var bounds = getRndBounds(lowerBound, upperBound);
        var randomizer = random.get();
        return randomizer.nextInt(bounds.lower, bounds.upper);
    }

    public static boolean randomizeBoolValue() {
        var randomizer = random.get();
        return randomizer.nextBoolean();
    }

    public enum StringFormat {
        DATE_TIME("date-time"),
        DATE("date"),
        UUID("uuid"),
        EMAIL("email"),
        URI("uri"),
        DEFAULT("default");

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
                case "uuid" -> UUID;
                case "email" -> EMAIL;
                case "uri" -> URI;
                default -> DEFAULT;
            };
        }
    }

    private static RndBounds getRndBounds(Integer lowerBound, Integer upperBound) {
        int lower = lowerBound == null ? 0 : lowerBound;
        int upper = upperBound == null ? 15 : upperBound;
        return new RndBounds(lower, upper);
    }

    private record RndBounds(int lower, int upper) {
    }
}
