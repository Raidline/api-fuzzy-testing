package pt.raidline.api.fuzzy.runner;

import pt.raidline.api.fuzzy.logging.CLILogger;
import pt.raidline.api.fuzzy.model.ApiDefinition;
import pt.raidline.api.fuzzy.runner.component.ComponentBuilder;
import pt.raidline.api.fuzzy.runner.graph.ComponentGraphNode;
import pt.raidline.api.fuzzy.runner.graph.SchemaProcessor;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static pt.raidline.api.fuzzy.assertions.AssertionUtils.precondition;

public class FuzzyTestProcessor {
    private final SchemaProcessor processor;
    private final Map<String, ComponentGraphNode> graphNodeMap = new HashMap<>(); // the Key is the property name

    public FuzzyTestProcessor() {
        this.processor = new SchemaProcessor();
    }

    public void process(ApiDefinition definition) {
        Objects.requireNonNull(definition);
        //ComponentBuilder.preBuild(entry.getKey(), entry.getValue()); -> this is what will be passed to each node

        precondition("Component key",
                "In order to proceed, you need to define the components part of the schema",
                () -> definition.components() != null);

        precondition("Component key",
                "In order to proceed, you need to define the schemas part of the components",
                () -> definition.components().schemas() != null && !definition.components().schemas().isEmpty());

        for (Map.Entry<String, ApiDefinition.Schema> entry : definition.components().schemas().entrySet()) {

            if (!graphNodeMap.containsKey(entry.getKey())) {
                ComponentGraphNode node = new ComponentGraphNode(entry.getKey());
                node.builder = ComponentBuilder.preBuild(entry.getKey(), entry.getValue(), s -> null);
                graphNodeMap.put(entry.getKey(), node);
            }


            var sup = ComponentBuilder.preBuild(entry.getKey(), entry.getValue(), s -> null);

            CLILogger.info("Body for schema : [%s] -> |%s|", entry.getKey(), sup.buildBody());
        }
    }
}
