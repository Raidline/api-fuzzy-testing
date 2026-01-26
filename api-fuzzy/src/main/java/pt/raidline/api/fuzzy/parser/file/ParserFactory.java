package pt.raidline.api.fuzzy.parser.file;

import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import pt.raidline.api.fuzzy.logging.CLILogger;

public final class ParserFactory {

    public static Parser decideParser(String file) {
        CLILogger.debug("Parsing schema file : %s", file);
        var extension = file.split("\\.")[1];

        if (extension.equalsIgnoreCase("yml") || extension.equalsIgnoreCase("yaml")) {
            return OpenAPIParser.of(new YAMLFactory(), file);
        }

        return OpenAPIParser.of(new MappingJsonFactory(), file);
    }
}
