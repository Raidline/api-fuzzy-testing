package pt.raidline.api.fuzzy.parser.args;

import static pt.raidline.api.fuzzy.assertions.AssertionUtils.aggregateErrors;
import static pt.raidline.api.fuzzy.assertions.AssertionUtils.precondition;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import pt.raidline.api.fuzzy.assertions.AssertionUtils;
import pt.raidline.api.fuzzy.logging.CLILogger;
import pt.raidline.api.fuzzy.model.AppArguments;

public final class ArgumentParser {

    private static final String NEEDED_PARAMS =
            "-f='filename';-s='localhost:xxxx'";

    private static final Map<String, ArgParser> MANDATORY_PARAMS = Map.of(
            "-f", new FileArgParser(),
            "-s", new ServerLocationArgParser()
    );

    //maybe not ArgParse here, but left it as it is now
    private static final Map<String, ArgParser> NON_MANDATORY_PARAMS = Map.of(
            "-t", new MaximunRunninTimeArgParser(),
            "-e", new EndingConditionArgParser(),
            "-r", new ConcurrentGateArgParser(),
            "-c", new ConcurrentEndpointCallsArgParser(),
            "-u", new UserExponentialGrowthArgParser()
    );

    private ArgumentParser() {
    }

    public static AppArguments parseArguments(String[] args) {
        precondition(
                "Parse Arguments",
                "You need to pass the following params %s".formatted(NEEDED_PARAMS),
                () -> args.length > 0
        );

        aggregateErrors("Parse Arguments")
                .onError("You need to pass the file for parsing : [-f]",
                        () -> paramExists(args, "-f"))
                .onError("You need to pass the server location : [-s]",
                        () -> paramExists(args, "-s"))
                .onError("You have more params than what is supported : [%s]".formatted(
                                args.length),
                        () -> args.length > MANDATORY_PARAMS.size() + NON_MANDATORY_PARAMS.size())
                .complete();

        var argBuilder = AppArguments.toBuilder();

        for (String arg : args) {
            var argToValue = arg.split("=");
            argBuilder = parseIfPresent(MANDATORY_PARAMS, argToValue, argBuilder);
            argBuilder = parseIfPresent(NON_MANDATORY_PARAMS, argToValue, argBuilder);
        }

        return argBuilder.build();
    }

    private static AppArguments.AppArgumentsBuilder parseIfPresent(Map<String, ArgParser> params,
                                                                   String[] arg,
                                                                   AppArguments.AppArgumentsBuilder builder) {
        if (params.containsKey(arg[0])) {
            return params.get(arg[0]).parse(builder, arg[0], arg[1]);
        }
        return builder;
    }

    private static boolean paramExists(String[] args, String param) {
        return Arrays.stream(args).anyMatch(s -> s.split("=")[0].equals(param));
    }

    private interface ArgParser {
        AppArguments.AppArgumentsBuilder parse(
                AppArguments.AppArgumentsBuilder builder,
                String argument,
                String value
        );
    }

    private static class FileArgParser implements ArgParser {

        @Override
        public AppArguments.AppArgumentsBuilder parse(
                AppArguments.AppArgumentsBuilder builder,
                String argument,
                String value
        ) {
            CLILogger.debug("schema file : %s,%s", argument, value);

            var f = new File(value);
            precondition(
                    "file exists",
                    "File %s does not exist".formatted(value),
                    f::exists
            );
            precondition(
                    "open file",
                    "File %s cannot be read".formatted(value),
                    f::canRead
            );

            return builder.addFile(argument, value);
        }
    }

    private static class ServerLocationArgParser implements ArgParser {

        @Override
        public AppArguments.AppArgumentsBuilder parse(
                AppArguments.AppArgumentsBuilder builder,
                String argument,
                String value
        ) {
            CLILogger.debug("server location : %s,%s", argument, value);

            //https://www.example.com:8080/
            //http://localhost:8080

            var aggregator = aggregateErrors("URL Validation");

            var endSchemeIndex = validateScheme(value, aggregator);

            validateHost(value, endSchemeIndex, aggregator);

            aggregator.complete();

            return builder.addServer(argument, value);
        }

        private void validateHost(String url, int endSchemeIndex, AssertionUtils.ErrorsAggregator aggregator) {
            var host = new String[]{url.substring(endSchemeIndex)};

            if (host[0].charAt(host[0].length() - 1) == '/') {
                host[0] = host[0].substring(
                        endSchemeIndex,
                        host[0].length() - 1
                );
            }

            aggregator.onError(
                    "URL [%s] does not contain a valid host".formatted(url),
                    () -> host[0].contains(":")
            );
        }

        private static int validateScheme(String url, AssertionUtils.ErrorsAggregator aggregator) {
            int schemeEndIndex = 5;
            int outIndex = schemeEndIndex;

            var scheme = url.substring(0, schemeEndIndex); // https = 5

            var lastSchemeChar = scheme.charAt(scheme.length() - 1);

            if (lastSchemeChar == 's') {
                outIndex += 3;
            } else {
                outIndex += 2; // in http by getting the 4 we are already at the :
                schemeEndIndex--;
            }

            var trailScheme = url.substring(schemeEndIndex, schemeEndIndex + 3); // ->://

            aggregator.onError("URL [%s] does not have a valid scheme".formatted(url),
                            () -> scheme.contains("http"))
                    .onError("URL [%s] does not have a valid scheme".formatted(url),
                            () -> trailScheme.equals("://"));

            return outIndex;
        }
    }

    private static class MaximunRunninTimeArgParser implements ArgParser {

        @Override
        public AppArguments.AppArgumentsBuilder parse(AppArguments.AppArgumentsBuilder builder, String argument,
                                                      String value) {
            try {
                var valueAsLong = Long.parseLong(value);

                precondition("Max Running Time Parsing",
                        "The value needs to be positive and higher than 0",
                        () -> valueAsLong > 0);

                return builder.addMaxTime(argument, valueAsLong);
            } catch (Exception e) {
                CLILogger.warn("You are passing a non numeric value as a max running time : [%s]", e.getMessage());
                precondition(
                        "Max Running Time Parsing",
                        "You are passing a non numeric value as a max running time : [%s]".formatted(value),
                        () -> false
                );
            }

            return builder;
        }
    }

    private static class EndingConditionArgParser implements ArgParser {

        private static final int[] POSSIBLE_HTTP_STATUS = new int[]{
                412, 500, 501, 502, 503, 504, 505
        };

        @Override
        public AppArguments.AppArgumentsBuilder parse(AppArguments.AppArgumentsBuilder builder, String argument,
                                                      String value) {

            try {
                var valueAsInt = Integer.parseInt(value);

                precondition("Ending Condition Parser",
                        "Possible values : [%s]".formatted(printPossibleStatusAsStrings()),
                        () -> containsValue(valueAsInt));

                return builder.addEndingCondition(argument, valueAsInt);
            } catch (Exception e) {
                CLILogger.warn("You are passing a non valid value as a ending condition : [%s]", e.getMessage());
                CLILogger.warn("Possible values : [%s]", printPossibleStatusAsStrings());
                precondition(
                        "Ending Condition Parser",
                        "You are passing a non valid value as a ending condition : [%s]".formatted(value),
                        () -> false
                );
            }

            return null;
        }

        private static String printPossibleStatusAsStrings() {
            return Arrays.stream(POSSIBLE_HTTP_STATUS)
                    .mapToObj(String::valueOf)
                    .collect(Collectors.joining(","));
        }

        private static boolean containsValue(int value) {
            for (int possibleHttpStatus : POSSIBLE_HTTP_STATUS) {
                if (value == possibleHttpStatus) {
                    return true;
                }
            }
            return false;
        }
    }

    private static class ConcurrentGateArgParser implements ArgParser {

        @Override
        public AppArguments.AppArgumentsBuilder parse(AppArguments.AppArgumentsBuilder builder, String argument, String value) {
            try {
                var valueAsInt = Integer.parseInt(value);

                precondition("Concurrent Gate Parser",
                        "The value needs to be positive and higher than 0",
                        () -> valueAsInt > 0);

                return builder.addConcurrentCallsGate(argument, valueAsInt);
            } catch (Exception e) {
                CLILogger.warn("You are passing a non valid value as a concurrent gate : [%s]", e.getMessage());
                precondition(
                        "Concurrent Gate Parser",
                        "You are passing a non valid value as a concurrent gate : [%s]".formatted(value),
                        () -> false
                );
            }

            return null;
        }
    }

    private static class ConcurrentEndpointCallsArgParser implements ArgParser {

        @Override
        public AppArguments.AppArgumentsBuilder parse(AppArguments.AppArgumentsBuilder builder, String argument, String value) {
            try {
                var valueAsInt = Integer.parseInt(value);

                precondition("Concurrent Endpoint Calls Parser",
                        "The value needs to be positive and higher than 0",
                        () -> valueAsInt > 0);

                return builder.addConcurrentEndpointCalls(argument, valueAsInt);
            } catch (Exception e) {
                CLILogger.warn("You are passing a non valid value as a concurrent endpoint calls : [%s]", e.getMessage());
                precondition(
                        "Concurrent Endpoint Calls Parser",
                        "You are passing a non valid value as a concurrent endpoint calls : [%s]".formatted(value),
                        () -> false
                );
            }

            return null;
        }
    }

    private static class UserExponentialGrowthArgParser implements ArgParser {

        @Override
        public AppArguments.AppArgumentsBuilder parse(AppArguments.AppArgumentsBuilder builder, String argument, String value) {
            try {
                var valueAsInt = Integer.parseInt(value);

                precondition("User Exponential Growth Parser",
                        "The value needs to be positive and higher than 0",
                        () -> valueAsInt > 0);

                return builder.addConcurrentEndpointCalls(argument, valueAsInt);
            } catch (Exception e) {
                CLILogger.warn("You are passing a non valid value as a user exponential growth : [%s]", e.getMessage());
                precondition(
                        "User Exponential Growth Parser",
                        "You are passing a non valid value as a user exponential growth : [%s]".formatted(value),
                        () -> false
                );
            }

            return null;
        }
    }
}
