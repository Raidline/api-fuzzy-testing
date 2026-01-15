package pt.raidline.api.fuzzy.runner.graph;

import pt.raidline.api.fuzzy.model.ApiDefinition;
import pt.raidline.api.fuzzy.runner.component.ComponentBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static pt.raidline.api.fuzzy.runner.component.ComponentBuilder.trimSchemaKeyFromRef;

public class SchemaProcessor {
    public final Map<String, ComponentGraphNode> componentGraphNodes = new HashMap<>();

    // For now, if we see cycles we just don't calculate the JSON again
    private ComponentGraphNode processSchemaProp(String key, ApiDefinition.Schema currSchema,
                                                 Map<String, ApiDefinition.Schema> schemaDefinition) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(currSchema, "Schema is null for key [%s]".formatted(key));
        Objects.requireNonNull(schemaDefinition);

        if (componentGraphNodes.containsKey(key)) {
            return componentGraphNodes.get(key);
        }

        ComponentGraphNode node = new ComponentGraphNode(key);

        node.builder = ComponentBuilder.preBuild(key, currSchema);

        if (currSchema.properties() != null) {
            for (var value : currSchema.properties().values()) {
                ifReferenceProperty(value, schema -> {
                    var normalizedSchemaKey = trimSchemaKeyFromRef(schema.$ref());
                    ComponentGraphNode graphNode = processSchemaProp(normalizedSchemaKey,
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

    public void processSchemaNodeGraph(Map<String, ApiDefinition.Schema> schemaDefinition) {
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
