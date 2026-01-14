package pt.raidline.api.fuzzy.runner.component;

public sealed interface SchemaBuilder permits ComponentBuilder.SchemaObjectBuilder,
        ComponentBuilder.SchemaArrayBuilder, ComponentBuilder.SchemaSingleBuilder {
    String buildBody();
}
