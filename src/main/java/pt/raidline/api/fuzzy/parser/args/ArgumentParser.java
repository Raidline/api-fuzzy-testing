package pt.raidline.api.fuzzy.parser.args;

import static pt.raidline.api.fuzzy.assertions.AssertionUtils.precondition;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
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

    private ArgumentParser() {}

    public static AppArguments parseArguments(String[] args) {
        precondition(
            "Parse Arguments",
            "You need to pass the following params %s".formatted(NEEDED_PARAMS),
            () -> args.length > 0
        );

        //todo: here we could build an aggregator to pass all the errors maybe?
        precondition(
            "Parse Arguments",
            "You need to pass the file for parsing : [-f]",
            () -> existsParam(args, "-f")
        );

        precondition(
            "Parse Arguments",
            "You need to pass the server location : [-s]",
            () -> existsParam(args, "-s")
        );

        precondition(
            "Parse Arguments",
            "You have more params than what is supported : [%s]".formatted(
                args.length
            ),
            () ->
                args.length ==
                MANDATORY_PARAMS.size() + NON_MANDATORY_PARAMS.size()
        );

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

    private static boolean existsParam(String[] args, String param) {
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

            var endSchemeIndex = validateScheme(value);

            validateHost(value, endSchemeIndex);

            return builder.addServer(argument, value);
        }

        private void validateHost(String url, int endSchemeIndex) {
            CLILogger.debug("validating host URL : %s", url);
            var host = new String[] { url.substring(endSchemeIndex) };

            CLILogger.debug("getting host : %s", host[0]);

            if (host[0].charAt(host[0].length() - 1) == '/') {
                host[0] = host[0].substring(
                    endSchemeIndex,
                    host[0].length() - 1
                );
            }

            precondition(
                "URL Validation",
                "URL %s does not contain a valid host".formatted(host[0]),
                () -> host[0].contains(":")
            );
        }

        private static int validateScheme(String url) {
            CLILogger.debug("validating URL : %s", url);

            int schemeEndIndex = 5;
            int outIndex = schemeEndIndex;

            var scheme = url.substring(0, schemeEndIndex); // https = 5

            CLILogger.debug("getting scheme : %s", scheme);

            var lastSchemeChar = scheme.charAt(scheme.length() - 1);

            if (lastSchemeChar == 's') {
                outIndex += 3;
            } else {
                outIndex += 2; // in http by getting the 4 we are already at the :
                schemeEndIndex--;
            }

            var trailScheme = url.substring(schemeEndIndex, schemeEndIndex + 3); // ->://

            CLILogger.debug("validating trailScheme : %s", trailScheme);

            //todo: again aggregation?
            precondition(
                "URL Validation",
                "URL %s does not have a valid scheme".formatted(url),
                () -> scheme.contains("http")
            );

            precondition(
                "URL Validation",
                "URL %s does not have a valid scheme".formatted(url),
                () -> trailScheme.equals("://")
            );

            return outIndex;
        }
    }
}
