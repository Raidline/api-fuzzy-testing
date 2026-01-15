package pt.raidline.api.fuzzy;

import pt.raidline.api.fuzzy.parser.args.ArgumentParser;
import pt.raidline.api.fuzzy.parser.file.OpenAPIParser;

public class ApiFuzzyMain {

    public static void main(String[] args) {
        var arguments = ArgumentParser.parseArguments(args);
        var def = new OpenAPIParser().parse(arguments.file.value());

        new FuzzyTestService().process(def);

        //CLILogger.debug("API Definition : %s", def.toString());
    }
}
