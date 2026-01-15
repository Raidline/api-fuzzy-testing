package pt.raidline.api.fuzzy;

import pt.raidline.api.fuzzy.model.ApiDefinition;
import pt.raidline.api.fuzzy.processors.paths.PathProcessor;
import pt.raidline.api.fuzzy.processors.schema.SchemaProcessor;

import java.util.Objects;

class FuzzyTestService {
    private final SchemaProcessor schemaProcessor;
    private final PathProcessor pathProcessor;

    FuzzyTestService() {
        this.pathProcessor = new PathProcessor();
        this.schemaProcessor = new SchemaProcessor();
    }

    void process(ApiDefinition definition) {
        Objects.requireNonNull(definition);

        this.schemaProcessor.processSchemaNodeGraph(definition.components());

        //this.schemaProcessor.componentGraphNodes.forEach((k, v) -> CLILogger.info("Body for schema : [%s] -> %s", k, v));

        this.pathProcessor.processPaths(definition.paths());
    }
}
