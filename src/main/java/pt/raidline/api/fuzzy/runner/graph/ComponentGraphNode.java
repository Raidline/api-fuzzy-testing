package pt.raidline.api.fuzzy.runner.graph;

import pt.raidline.api.fuzzy.runner.component.SchemaBuilder;

import java.util.List;

import static pt.raidline.api.fuzzy.assertions.AssertionUtils.precondition;

public class ComponentGraphNode { //per property can be better! this way we can build one at the time
    public final String key;
    public SchemaBuilder builder;
    private List<ComponentGraphNode> connections;

    public ComponentGraphNode(String key) {
        this.key = key;
    }

    public String buildSchema() {
        return this.builder.buildBody();
    }

    public void addConnection(ComponentGraphNode node) {
        this.connections.add(node);
    }
}
