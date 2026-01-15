package pt.raidline.api.fuzzy.runner.component;

import java.util.function.UnaryOperator;

public sealed interface SchemaBuilder permits ComponentBuilder.SchemaObjectBuilder,
        ComponentBuilder.SchemaArrayBuilder, ComponentBuilder.SchemaSingleBuilder {
    String buildBody(UnaryOperator<String> onRefFound);
}
