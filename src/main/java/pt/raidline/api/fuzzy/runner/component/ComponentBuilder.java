package pt.raidline.api.fuzzy.runner.component;

import pt.raidline.api.fuzzy.assertions.AssertionUtils;
import pt.raidline.api.fuzzy.logging.CLILogger;
import pt.raidline.api.fuzzy.model.ApiDefinition;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static pt.raidline.api.fuzzy.assertions.AssertionUtils.precondition;

public final class ComponentBuilder {

    public static SchemaBuilder preBuild(String key, ApiDefinition.Schema schema) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(schema);

        return switch (schema.type()) {
            case OBJECT -> new SchemaObjectBuilder(key, schema);
            case STRING, INTEGER -> new SchemaSingleBuilder(key, schema);
            case ARRAY -> new SchemaArrayBuilder(key, schema);
        };
    }

    record SchemaObjectBuilder(String key, ApiDefinition.Schema schema) implements SchemaBuilder {

        @Override
        public String buildBody() {
            AssertionUtils.internalAssertion("Properties on Schema",
                    () -> schema.properties() != null && !schema.properties().isEmpty());
            StringBuilder innerObj = new StringBuilder("{\n");
            //todo: maybe we can pass the graph here and find if it's a ref..
            for (var entry : schema.properties().entrySet()) {
                ApiDefinition.Schema value = entry.getValue();

                if (value.$ref() != null) {
                    CLILogger.warn("We have a cross-reference, skip for now : [%s]", entry.getKey());

                    continue;
                }

                if (value.type().isArray()) {
                    //todo: this ref comparison should be done in the layer above to get the respective node in the
                    // graph
                    ApiDefinition.Schema arrayItem = value.items();
                    precondition("Schema Validation", "Property [%s] array type must contain [items]"
                            .formatted(entry.getKey()), () -> arrayItem != null);

                    if (arrayItem.$ref() != null) {
                        CLILogger.warn("We have a cross-reference inside the array, skip for now : [%s]",
                                arrayItem.$ref());
                    } else {
                        int size = 5;
                        appendProperty(entry.getKey(), innerObj);
                        buildSimpleArrayValues(entry.getKey(), size, innerObj, arrayItem);
                    }
                } else {
                    appendProperty(entry.getKey(), innerObj);
                    innerObj.append(buildValue(entry.getKey(), value)); //{value}
                }

                innerObj.append(",").append("\n");
            }
            innerObj.deleteCharAt(innerObj.length() - 2); //remove last ","

            return innerObj.append("}").toString();
        }
    }

    record SchemaArrayBuilder(String key, ApiDefinition.Schema schema) implements SchemaBuilder {

        @Override
        public String buildBody() {
            int outerArraySize = 5;
            CLILogger.debug("Building an array");
            var body = new StringBuilder();
            ApiDefinition.Schema arrayItem = schema.items();
            precondition("Schema Validation", "Property [%s] array type must contain [items]"
                    .formatted(key), () -> arrayItem != null);

            if (arrayItem.$ref() != null) {
                //todo: this ref comparison should be done in the layer above to get the respective node in the
                // graph
                CLILogger.warn("We have a cross-reference inside the array, skip for now : [%s]",
                        arrayItem.$ref());
            } else {
                buildSimpleArrayValues(key, outerArraySize, body, arrayItem);
            }
            return body.toString();
        }
    }

    record SchemaSingleBuilder(String key, ApiDefinition.Schema schema) implements SchemaBuilder {

        @Override
        public String buildBody() {
            StringBuilder value = new StringBuilder();
            appendProperty(key, value);
            value.append(buildValue(key, schema));

            return value.toString();
        }
    }

    static void appendProperty(String key,
                               StringBuilder body) {
        body.append("\"").append(key).append("\":"); // "id:"
    }

    static void buildSimpleArrayValues(String key, int outerArraySize, StringBuilder body,
                                       ApiDefinition.Schema arrayItem) {
        body.append("[");
        for (int i = 0; i < outerArraySize; i++) {
            body.append(buildValue(key, arrayItem));

            if (i != outerArraySize - 1) {
                body.append(",");
            }
        }
        body.append("]");
    }


    static Object buildValue(String key, ApiDefinition.Schema schema) {
        return switch (schema.type()) {
            case INTEGER -> 1;
            case STRING -> {
                if ("date-time".equalsIgnoreCase(schema.format())) {
                    yield LocalDateTime.now().toString();
                }

                yield "some string";
            }
            case ARRAY -> {
                List<Object> values = new ArrayList<>();
                int size = 5;

                for (int i = 0; i < size; i++) {
                    values.add(buildValue(key, schema.items()));
                }

                yield values;
            }
            case OBJECT -> new SchemaObjectBuilder(key, schema).buildBody();
        };
    }
}
