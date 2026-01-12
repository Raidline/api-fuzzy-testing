package pt.raidline.api.fuzzy.parser.args;

import pt.raidline.api.fuzzy.logging.CLILogger;
import pt.raidline.api.fuzzy.model.AppArguments;

import java.util.Arrays;
import java.util.Map;

import static pt.raidline.api.fuzzy.assertions.AssertionUtils.precondition;

public final class ArgumentParser {

    private static final String NEEDED_PARAMS = "-f='filename';-s='localhost:xxxx'";

    private static final Map<String, ArgParser> MANDATORY_PARAMS = Map.of(
            "-f", new FileArgParser(),
            "-s", new ServerLocationArgParser()
    );

    //maybe not ArgParse here, but left it as it is now
    private static final Map<String, ArgParser> NON_MANDATORY_PARAMS = Map.of();

    private ArgumentParser() {
    }

    public static AppArguments parseArguments(String[] args) {

        //todo: here we could build an aggregator to pass all the errors maybe?
        precondition("Parse Arguments",
                "You need to pass the following params %s".formatted(NEEDED_PARAMS),
                () -> args.length > 0);

        precondition("Parse Arguments",
                "You need to pass the file for parsing : [-f]",
                () -> existsParam(args, "-f"));

        precondition("Parse Arguments",
                "You need to pass the server location : [-s]",
                () -> existsParam(args, "-s"));

        precondition("Parse Arguments",
                "You have more params than what is supported : [%s]".formatted(args.length),
                () -> args.length == MANDATORY_PARAMS.size() + NON_MANDATORY_PARAMS.size());


        var argBuilder = AppArguments.toBuilder();

        for (String arg : args) {
            var argToValue = arg.split("=");
            MANDATORY_PARAMS.get(argToValue[0]).parse(argBuilder, argToValue[0], argToValue[1]);
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
                String value);
    }

    private static class FileArgParser implements ArgParser {

        @Override
        public AppArguments.AppArgumentsBuilder parse(AppArguments.AppArgumentsBuilder builder, String argument, String value) {
            CLILogger.debug("schema file : %s,%s", argument, value);

            //todo: add validations here maybe?

            return builder.addFile(argument, value);
        }
    }

    private static class ServerLocationArgParser implements ArgParser {

        @Override
        public AppArguments.AppArgumentsBuilder parse(AppArguments.AppArgumentsBuilder builder, String argument, String value) {
            CLILogger.debug("server location : %s,%s", argument, value);

            //todo: add more validations, like can we parse this URL??

            return builder.addServer(argument, value);
        }
    }
}
