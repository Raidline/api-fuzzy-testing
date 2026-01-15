package pt.raidline.api.fuzzy.runner.component;

import pt.raidline.api.fuzzy.assertions.AssertionUtils;
import pt.raidline.api.fuzzy.model.ApiDefinition;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static pt.raidline.api.fuzzy.assertions.AssertionUtils.precondition;

public final class ComponentBuilder {

    public static SchemaBuilder preBuild(String key,
                                         ApiDefinition.Schema schema) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(schema);

        return switch (schema.type()) {
            case OBJECT -> new SchemaObjectBuilder(key, schema);
            case STRING, INTEGER, BOOLEAN -> new SchemaSingleBuilder(key, schema); //do we need this?
            case ARRAY -> new SchemaArrayBuilder(key, schema);
        };
    }

    record SchemaObjectBuilder(String key, ApiDefinition.Schema schema) implements SchemaBuilder {

        @Override
        public String buildBody(UnaryOperator<String> onRef) {
            AssertionUtils.internalAssertion("Properties on Schema",
                    () -> schema.properties() != null && !schema.properties().isEmpty());
            StringBuilder innerObj = new StringBuilder("{\n");
            for (var entry : schema.properties().entrySet()) {
                ApiDefinition.Schema value = entry.getValue();
                appendProperty(entry.getKey(), innerObj);

                if (value.$ref() != null) {
                    var key = trimSchemaKeyFromRef(value.$ref());
                    innerObj.append(onRef.apply(key));
                } else if (value.type().isArray()) {
                    int size = 5;
                    ApiDefinition.Schema arrayItem = value.items();
                    precondition("Schema Validation", "Property [%s] array type must contain [items]"
                            .formatted(entry.getKey()), () -> arrayItem != null);

                    if (arrayItem.$ref() != null) {
                        String k = trimSchemaKeyFromRef(arrayItem.$ref());
                        buildArrayValues(size, innerObj, () -> onRef.apply(k));
                    } else {
                        buildArrayValues(size, innerObj, () ->
                                buildValue(entry.getKey(), arrayItem, onRef));
                    }
                } else {
                    innerObj.append(buildValue(entry.getKey(), value, onRef)); //{value}
                }

                innerObj.append(",").append("\n");
            }
            innerObj.deleteCharAt(innerObj.length() - 2); //remove last ","

            return innerObj.append("}").toString();
        }
    }

    record SchemaArrayBuilder(String key, ApiDefinition.Schema schema) implements SchemaBuilder {

        @Override
        public String buildBody(UnaryOperator<String> onRef) {
            var body = new StringBuilder();
            int outerArraySize = 5;
            ApiDefinition.Schema arrayItem = schema.items();
            precondition("Schema Validation", "Property [%s] array type must contain [items]"
                    .formatted(key), () -> arrayItem != null);

            appendProperty(key, body);

            if (arrayItem.$ref() != null) {
                String k = trimSchemaKeyFromRef(arrayItem.$ref());
                buildArrayValues(outerArraySize, body, () -> onRef.apply(k));
            } else {
                buildArrayValues(outerArraySize, body, () -> buildValue(key, arrayItem, onRef));
            }
            return body.toString();
        }
    }

    record SchemaSingleBuilder(String key, ApiDefinition.Schema schema) implements SchemaBuilder {

        @Override
        public String buildBody(UnaryOperator<String> onRef) {
            StringBuilder value = new StringBuilder();
            appendProperty(key, value);
            value.append(buildValue(key, schema, onRef));

            return value.toString();
        }
    }

    // #/components/schemas/Tag
    public static String trimSchemaKeyFromRef(String ref) {
        String[] split = ref.split("/");
        return split[split.length - 1];
    }

    static void appendProperty(String key,
                               StringBuilder body) {
        body.append("\"").append(key).append("\":"); // "id:"
    }

    static void buildArrayValues(int outerArraySize, StringBuilder body,
                                 Supplier<Object> onItem) {
        body.append("[");
        for (int i = 0; i < outerArraySize; i++) {
            body.append(onItem.get());

            if (i != outerArraySize - 1) {
                body.append(",");
            }
        }
        body.append("]");
    }


    //todo: apply random shit logic here
    static Object buildValue(String key, ApiDefinition.Schema schema, UnaryOperator<String> onRef) {
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
                    values.add(buildValue(key, schema.items(), onRef));
                }

                yield values;
            }
            case OBJECT -> new SchemaObjectBuilder(key, schema).buildBody(onRef);
            case BOOLEAN -> true;
        };
    }
}
