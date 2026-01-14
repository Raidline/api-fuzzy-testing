package pt.raidline.api.fuzzy.runner.graph;

import pt.raidline.api.fuzzy.runner.component.SchemaBuilder;

import static pt.raidline.api.fuzzy.assertions.AssertionUtils.precondition;

public class ComponentGraphNode { //per property can be better! this way we can build one at the time
    private final SchemaBuilder builder;
    private final ComponentGraphNode next;

    public ComponentGraphNode(SchemaBuilder builder) {
        this(builder, null);
    }

    public ComponentGraphNode(SchemaBuilder builder,
                              ComponentGraphNode next) {
        this.builder = builder;
        this.next = next;
    }

    public String buildSchema() {
        return this.builder.buildBody();
    }

    public SchemaBuilder goToNext() {
        precondition("Next node in graph",
                "We are calling goToNext, there needs to be a next",
                () -> this.next != null);

        return this.next.builder;
    }
}
