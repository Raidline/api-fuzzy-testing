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
    private final TestController controller;

    FuzzyService(TestController controller) {
        this.controller = controller;
        this.pathProcessor = new PathProcessor();
        this.schemaProcessor = new SchemaProcessor();
    }

    void process(ApiDefinition definition, AppArguments args) {
        Objects.requireNonNull(definition);
        Objects.requireNonNull(args);

        var schemaGraph = this.schemaProcessor.processSchemaNodeGraph(definition.components());
        var paths = this.pathProcessor.processPaths(definition.paths());
        new FuzzyClient(args, controller).executeRequest(schemaGraph, paths, args);
    }
}
