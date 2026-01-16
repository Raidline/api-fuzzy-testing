package pt.raidline.api.fuzzy;

import pt.raidline.api.fuzzy.client.FuzzyClient;
import pt.raidline.api.fuzzy.model.ApiDefinition;
import pt.raidline.api.fuzzy.model.AppArguments;
import pt.raidline.api.fuzzy.processors.paths.PathProcessor;
import pt.raidline.api.fuzzy.processors.schema.SchemaProcessor;

import java.util.Objects;

class FuzzyService {
    private final SchemaProcessor schemaProcessor;
    private final PathProcessor pathProcessor;
    private final FuzzyClient client;

    FuzzyService() {
        this.pathProcessor = new PathProcessor();
        this.schemaProcessor = new SchemaProcessor();
        this.client = new FuzzyClient();
    }

    void process(ApiDefinition definition, AppArguments.Arg server) {
        Objects.requireNonNull(definition);
        Objects.requireNonNull(server);

        var schemaGraph = this.schemaProcessor.processSchemaNodeGraph(definition.components());
        //this.schemaProcessor.componentGraphNodes.forEach((k, v) -> CLILogger.info("Body for schema : [%s] -> %s", k, v));
        var paths = this.pathProcessor.processPaths(definition.paths());
        this.client.executeRequest(schemaGraph, paths, server);
    }
}
