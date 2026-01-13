package pt.raidline.api.fuzzy.runner;

import pt.raidline.api.fuzzy.logging.CLILogger;
import pt.raidline.api.fuzzy.model.ApiDefinition;

import java.util.Map;

public class FuzzyTestProcessor {
    private final ApiDefinition definition;
    private final ComponentBuilder builder;

    public FuzzyTestProcessor(ApiDefinition definition) {
        this.definition = definition;
        this.builder = new ComponentBuilder();
    }


    public void process() {
        for (Map.Entry<String, ApiDefinition.Schema> entry : definition.components().schemas().entrySet()) {
            var sup = builder.preBuild(entry.getKey(), entry.getValue());

            CLILogger.info("Body for schema : [%s] -> |%s|", entry.getKey(), sup.get());
        }
    }
}
