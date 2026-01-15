package pt.raidline.api.fuzzy.runner;

import pt.raidline.api.fuzzy.logging.CLILogger;
import pt.raidline.api.fuzzy.model.ApiDefinition;
import pt.raidline.api.fuzzy.runner.graph.SchemaProcessor;

import java.util.Objects;

import static pt.raidline.api.fuzzy.assertions.AssertionUtils.precondition;

public class FuzzyTestProcessor {
    private final SchemaProcessor processor;

    public FuzzyTestProcessor() {
        this.processor = new SchemaProcessor();
    }

    public void process(ApiDefinition definition) {
        Objects.requireNonNull(definition);

        precondition("Component key",
                "In order to proceed, you need to define the components part of the schema",
                () -> definition.components() != null);

        precondition("Component key",
                "In order to proceed, you need to define the schemas part of the components",
                () -> definition.components().schemas() != null && !definition.components().schemas().isEmpty());

        this.processor.processSchemaNodeGraph(definition.components().schemas());
        this.processor.componentGraphNodes.forEach((k, v) -> CLILogger.info("Body for schema : [%s] -> %s", k, v));
    }
}
