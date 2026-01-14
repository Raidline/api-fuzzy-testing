package pt.raidline.api.fuzzy.runner.graph;

import pt.raidline.api.fuzzy.assertions.AssertionUtils;
import pt.raidline.api.fuzzy.model.ApiDefinition;
import pt.raidline.api.fuzzy.runner.component.ComponentBuilder;
import pt.raidline.api.fuzzy.runner.component.SchemaBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SchemaProcessor {
    private final Map<String, ComponentGraphNode> componentGraphNodes = new HashMap<>();
    private final Map<String, Set<String>> dependencyCounter = new HashMap<>();

    //todo: build the graph first, with all the relations.
    //todo: we need to know the components we have seen and assign them to the next if they appear again
    //todo: if there is a $ref and we have not seen it we create the node and put in the seen

    // For now, if we see cycles we just don't calculate the JSON again (seen array in DFS)

    public void processSchemaNodeGraph(Map<String, ApiDefinition.Schema> schema) {
        for (var entry : schema.entrySet()) {
            if (!componentGraphNodes.containsKey(entry.getKey())) {
                ComponentGraphNode node = new ComponentGraphNode(entry.getKey());

                node.builder = ComponentBuilder.preBuild(
                        entry.getKey(), entry.getValue(),
                        //todo: this needs to be recursive, because we need to find the god damn reference and build that
                        k -> {
                            // build this one and add as a connection
                            if (!componentGraphNodes.containsKey(k)) {
                                ComponentGraphNode n2 = new ComponentGraphNode(k);

                                ApiDefinition.Schema newSchema = this.findSchema(k, schema);
                                node.builder = ComponentBuilder.preBuild(k, newSchema, k1 -> {/* repeat*/
                                    return null;
                                });

                                componentGraphNodes.put(k, n2);

                                node.addConnection(n2);

                                /*todo: i think this is redundant has i'm doing {
                                 innerObj.append(new SchemaObjectBuilder(
                            key,
                            this.onReference.onReferenceFound(key),
                            onReference
                    ).buildBody());
                    }

                    which basically creates a new object at that time, this is not generating the graph i wanted

                    Maybe i want each SchemaxxxBuilder have a list (per-order) of the refs? - this makes kinda of a graph
                    Or maybe i find a way to have a lazy function in the ref parts, where i get the reference from the node

                                 */
                                return newSchema;
                            } else {
                                return componentGraphNodes.get(k).builder;
                            }
                        }
                );

                componentGraphNodes.put(entry.getKey(), node);
            }
        }
    }

    private ApiDefinition.Schema findSchema(String k, Map<String, ApiDefinition.Schema> schema) {
        AssertionUtils.precondition("Component in Schema",
                "You are referencing a component that does not exist",
                () -> schema.containsKey(k));
        return schema.get(k);
    }
}
