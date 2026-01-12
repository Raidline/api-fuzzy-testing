package pt.raidline.api.fuzzy;

import pt.raidline.api.fuzzy.logging.CLILogger;
import pt.raidline.api.fuzzy.parser.args.ArgumentParser;
import pt.raidline.api.fuzzy.parser.file.OpenAPIParser;

public class ApiFuzzyMain {

    public static void main(String[] args) {
        var arguments = ArgumentParser.parseArguments(args);

        CLILogger.info("schema file : %s", args[0]);

        var def = new OpenAPIParser().parse(arguments.file.value());

        //CLILogger.info("API Definition : %s", def.toString());
    }
}
