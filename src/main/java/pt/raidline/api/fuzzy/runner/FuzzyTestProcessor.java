package pt.raidline.api.fuzzy.runner;

import pt.raidline.api.fuzzy.assertions.AssertionUtils;
import pt.raidline.api.fuzzy.logging.CLILogger;
import pt.raidline.api.fuzzy.model.ApiDefinition;
import pt.raidline.api.fuzzy.runner.component.ComponentBuilder;
import pt.raidline.api.fuzzy.runner.graph.ComponentGraphNode;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static pt.raidline.api.fuzzy.assertions.AssertionUtils.precondition;

public class FuzzyTestProcessor {
    private final ApiDefinition definition;
    private final Map<String, ComponentGraphNode> graphNodeMap = new HashMap<>(); // the Key is the property name

    public FuzzyTestProcessor(ApiDefinition definition) {
        Objects.requireNonNull(definition);
        this.definition = definition;
    }

    public void process() {
        //ComponentBuilder.preBuild(entry.getKey(), entry.getValue()); -> this is what will be passed to each node

        precondition("Component key",
                "In order to proceed, you need to define the components part of the schema",
                () -> this.definition.components() != null);

        precondition("Component key",
                "In order to proceed, you need to define the schemas part of the components",
                () -> this.definition.components().schemas() != null && !this.definition.components().schemas().isEmpty());


        //todo: build the graph first, with all the relations.
        //todo: we need to know the components we have seen and assign them to the next if they appear again
        //todo: if there is a $ref and we have not seen it we create the node and put in the seen

        // For now, if we see cycles we just don't calculate the JSON again (seen array in DFS)

        for (Map.Entry<String, ApiDefinition.Schema> entry : definition.components().schemas().entrySet()) {

            if (!graphNodeMap.containsKey(entry.getKey())) {
                ComponentGraphNode node = new ComponentGraphNode(
                        ComponentBuilder.preBuild(entry.getKey(), entry.getValue()));

                graphNodeMap.put(entry.getKey(), node);
            }


            var sup = ComponentBuilder.preBuild(entry.getKey(), entry.getValue());

            CLILogger.info("Body for schema : [%s] -> |%s|", entry.getKey(), sup.buildBody());
        }
    }
}
