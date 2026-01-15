package pt.raidline.api.fuzzy;

import pt.raidline.api.fuzzy.logging.CLILogger;
import pt.raidline.api.fuzzy.model.ApiDefinition;
import pt.raidline.api.fuzzy.graph.SchemaProcessor;

import java.util.Objects;

import static pt.raidline.api.fuzzy.assertions.AssertionUtils.precondition;

class FuzzyTestService {
    private final SchemaProcessor schemaProcessor;

    FuzzyTestService() {
        this.schemaProcessor = new SchemaProcessor();
    }

    void process(ApiDefinition definition) {
        Objects.requireNonNull(definition);

        precondition("Component key",
                "In order to proceed, you need to define the components part of the schema",
                () -> definition.components() != null);

        precondition("Component key",
                "In order to proceed, you need to define the schemas part of the components",
                () -> definition.components().schemas() != null && !definition.components().schemas().isEmpty());

        this.schemaProcessor.processSchemaNodeGraph(definition.components().schemas());


        this.schemaProcessor.componentGraphNodes.forEach((k, v) -> CLILogger.info("Body for schema : [%s] -> %s", k, v));
    }
}
