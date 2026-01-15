package pt.raidline.api.fuzzy.processors.schema;

import pt.raidline.api.fuzzy.assertions.AssertionUtils;
import pt.raidline.api.fuzzy.model.ApiDefinition;
import pt.raidline.api.fuzzy.component.ComponentBuilder;
import pt.raidline.api.fuzzy.processors.schema.model.SchemaBuilderNode;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import static pt.raidline.api.fuzzy.assertions.AssertionUtils.precondition;
import static pt.raidline.api.fuzzy.component.ComponentBuilder.trimSchemaKeyFromRef;

public class SchemaProcessor {
    public final Map<String, SchemaBuilderNode> componentGraphNodes = new HashMap<>();

    // For now, if we see cycles we just don't calculate the JSON again
    private SchemaBuilderNode processSchemaProp(String key, ApiDefinition.Schema currSchema,
                                                Map<String, ApiDefinition.Schema> schemaDefinition) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(currSchema, "Schema is null for key [%s]".formatted(key));
        Objects.requireNonNull(schemaDefinition);

        if (componentGraphNodes.containsKey(key)) {
            return componentGraphNodes.get(key);
        }

        SchemaBuilderNode node = new SchemaBuilderNode(key, ComponentBuilder.preBuild(key, currSchema));

        if (currSchema.properties() != null) {
            for (var value : currSchema.properties().values()) {
                ifReferenceProperty(value, schema -> {
                    var normalizedSchemaKey = trimSchemaKeyFromRef(schema.$ref());
                    SchemaBuilderNode graphNode = processSchemaProp(normalizedSchemaKey,
                            schemaDefinition.get(normalizedSchemaKey), schemaDefinition);
                    if (graphNode != null) {
                        node.connections.put(normalizedSchemaKey, graphNode);
                    }
                });
            }
        }

        componentGraphNodes.put(key, node);

        return node;
    }

    public void processSchemaNodeGraph(ApiDefinition.Components components) {
        precondition("Component key",
                "In order to proceed, you need to define the components part of the schema",
                () -> components != null);

        precondition("Component key",
                "In order to proceed, you need to define the schemas part of the components",
                () -> components.schemas() != null && !components.schemas().isEmpty());

        var schemaDefinition = components.schemas();

        AssertionUtils.internalAssertion("Schema definition", () -> schemaDefinition != null);

        for (var entry : schemaDefinition.entrySet()) {
            this.processSchemaProp(entry.getKey(), entry.getValue(), schemaDefinition);
        }
    }

    private void ifReferenceProperty(ApiDefinition.Schema value, Consumer<ApiDefinition.Schema> schema) {
        if (value.type() != null && value.type().isArray() && value.items().$ref() != null) {
            schema.accept(value.items());
        } else if (value.$ref() != null) {
            schema.accept(value);
        }
    }
}
