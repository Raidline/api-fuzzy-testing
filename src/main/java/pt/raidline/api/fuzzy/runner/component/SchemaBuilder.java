package pt.raidline.api.fuzzy.runner.component;

public sealed interface SchemaBuilder permits ComponentBuilder.SchemaObjectBuilder,
        ComponentBuilder.SchemaArrayBuilder, ComponentBuilder.SchemaSingleBuilder {
    //todo: maybe i want to have a preBuild here as well.
    //where i lazy construct the entire thing
    //todo: HOW DO I SIGNAL THAT A PART OF THE JSON WILL BE DONE LATTER? WE NEED TO HAVE A LAZY FUNCTION IN THERE
    //todo: we need to put some kind of placeholder there to replace after

    //todo: !draw this logic!

    // and the buildBody receives a callback to give the correct Builder that will handle the refs
    String buildBody();
}
