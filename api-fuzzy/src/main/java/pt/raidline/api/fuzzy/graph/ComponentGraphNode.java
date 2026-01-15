package pt.raidline.api.fuzzy.graph;

import pt.raidline.api.fuzzy.component.SchemaBuilder;

import java.util.HashMap;
import java.util.Map;

public class ComponentGraphNode {
    private final String key;
    public SchemaBuilder builder;
    public final Map<String, ComponentGraphNode> connections = new HashMap<>();

    public ComponentGraphNode(String key) {
        this.key = key;
    }

    public String buildSchema() {
        return this.builder.buildBody(k -> this.connections.get(k).buildSchema());
    }

    @Override
    public String toString() {
        return this.buildSchema();
    }
}
