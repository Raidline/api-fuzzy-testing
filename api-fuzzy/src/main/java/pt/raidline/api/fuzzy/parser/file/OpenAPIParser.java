package pt.raidline.api.fuzzy.parser.file;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import pt.raidline.api.fuzzy.logging.CLILogger;
import pt.raidline.api.fuzzy.model.ApiDefinition;

import static pt.raidline.api.fuzzy.assertions.AssertionUtils.internalAssertion;
import static pt.raidline.api.fuzzy.assertions.AssertionUtils.precondition;

public class OpenAPIParser implements Parser {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    public ApiDefinition parse(String path) {
        CLILogger.debug("Parsing schema file : %s", path);

        var file = new File(path);

        precondition("file exists", "File %s does not exist".formatted(path), file::exists);
        precondition("open file", "File %s cannot be read".formatted(path), file::canRead);

        try {
            var apiDefinition = OBJECT_MAPPER.readValue(file, ApiDefinition.class);
            CLILogger.debug("Parsed file: %s", path);
            return apiDefinition;
        } catch (IOException e) {
            CLILogger.severe("Failed to parse OpenAPI file: %s", e.getMessage());
            internalAssertion("parse openapi file", () -> false);
            return null;
        }
    }
}
