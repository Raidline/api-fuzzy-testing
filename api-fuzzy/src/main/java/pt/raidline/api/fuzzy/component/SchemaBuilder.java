package pt.raidline.api.fuzzy.component;

import java.util.function.UnaryOperator;

public sealed interface SchemaBuilder permits ComponentBuilder.SchemaObjectBuilder,
        ComponentBuilder.SchemaArrayBuilder, ComponentBuilder.SchemaSingleBuilder {
    String buildBody(UnaryOperator<String> onRefFound);
}
