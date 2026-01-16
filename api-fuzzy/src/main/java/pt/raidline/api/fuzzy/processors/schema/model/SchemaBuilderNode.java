package pt.raidline.api.fuzzy.processors.schema.model;

import pt.raidline.api.fuzzy.processors.schema.component.SchemaBuilder;

import java.util.HashMap;
import java.util.Map;

public class SchemaBuilderNode {
    private final String key;
    public final SchemaBuilder builder;
    public final Map<String, SchemaBuilderNode> connections = new HashMap<>();

    public SchemaBuilderNode(String key, SchemaBuilder builder) {
        this.key = key;
        this.builder = builder;
    }

    public String buildSchema() {
        return this.builder.buildBody(k -> this.connections.get(k).buildSchema());
    }

    @Override
    public String toString() {
        return this.buildSchema();
    }
}
