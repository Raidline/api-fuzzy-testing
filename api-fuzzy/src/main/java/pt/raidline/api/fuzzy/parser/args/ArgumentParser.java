package pt.raidline.api.fuzzy.parser.args;

import static pt.raidline.api.fuzzy.assertions.AssertionUtils.aggregateErrors;
import static pt.raidline.api.fuzzy.assertions.AssertionUtils.precondition;

import java.io.File;
import java.util.Arrays;
import java.util.Map;

import pt.raidline.api.fuzzy.assertions.AssertionUtils;
import pt.raidline.api.fuzzy.logging.CLILogger;
import pt.raidline.api.fuzzy.model.AppArguments;

public final class ArgumentParser {

    private static final String NEEDED_PARAMS =
            "-f='filename';-s='localhost:xxxx'";

    private static final Map<String, ArgParser> MANDATORY_PARAMS = Map.of(
            "-f",
            new FileArgParser(),
            "-s",
            new ServerLocationArgParser()
    );

    //maybe not ArgParse here, but left it as it is now
    private static final Map<String, ArgParser> NON_MANDATORY_PARAMS = Map.of();

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
                        () -> args.length == MANDATORY_PARAMS.size() + NON_MANDATORY_PARAMS.size())
                .complete();

        var argBuilder = AppArguments.toBuilder();

        for (String arg : args) {
            var argToValue = arg.split("=");
            MANDATORY_PARAMS.get(argToValue[0]).parse(
                    argBuilder,
                    argToValue[0],
                    argToValue[1]
            );
        }

        return argBuilder.build();
    }

    private static boolean paramExists(String[] args, String param) {
        return Arrays.stream(args).anyMatch(s -> s.split("=")[0].equals(param));
    }

    private interface ArgParser {
        void parse(
                AppArguments.AppArgumentsBuilder builder,
                String argument,
                String value
        );
    }

    private static class FileArgParser implements ArgParser {

        @Override
        public void parse(
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

            builder.addFile(argument, value);
        }
    }

    private static class ServerLocationArgParser implements ArgParser {

        @Override
        public void parse(
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

            builder.addServer(argument, value);
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
}
