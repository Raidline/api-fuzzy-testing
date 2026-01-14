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

    public static SchemaBuilder preBuild(String key,
                                         ApiDefinition.Schema schema,
                                         OnReference onReference) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(schema);

        return switch (schema.type()) {
            case OBJECT -> new SchemaObjectBuilder(key, schema, onReference);
            case STRING, INTEGER -> new SchemaSingleBuilder(key, schema, onReference);
            case ARRAY -> new SchemaArrayBuilder(key, schema, onReference);
        };
    }

    record SchemaObjectBuilder(String key, ApiDefinition.Schema schema,
                               OnReference onReference) implements SchemaBuilder {

        @Override
        public String buildBody() {
            AssertionUtils.internalAssertion("Properties on Schema",
                    () -> schema.properties() != null && !schema.properties().isEmpty());
            StringBuilder innerObj = new StringBuilder("{\n");
            for (var entry : schema.properties().entrySet()) {
                ApiDefinition.Schema value = entry.getValue();

                if (value.$ref() != null) {
                    var key = trimSchemaKeyFromRef(value.$ref());
                    innerObj.append(new SchemaObjectBuilder(
                            key,
                            this.onReference.onReferenceFound(key),
                            onReference
                    ).buildBody());

                    continue;
                }

                if (value.type().isArray()) {
                    int size = 5;
                    ApiDefinition.Schema arrayItem = value.items();
                    precondition("Schema Validation", "Property [%s] array type must contain [items]"
                            .formatted(entry.getKey()), () -> arrayItem != null);

                    if (arrayItem.$ref() != null) {
                        buildArrayValues(entry.getKey(), size, innerObj,
                                this.onReference.onReferenceFound(trimSchemaKeyFromRef(arrayItem.$ref())),
                                onReference);
                    } else {
                        appendProperty(entry.getKey(), innerObj);
                        buildArrayValues(entry.getKey(), size, innerObj, arrayItem, onReference);
                    }
                } else {
                    appendProperty(entry.getKey(), innerObj);
                    innerObj.append(buildValue(entry.getKey(), value, onReference)); //{value}
                }

                innerObj.append(",").append("\n");
            }
            innerObj.deleteCharAt(innerObj.length() - 2); //remove last ","

            return innerObj.append("}").toString();
        }
    }

    record SchemaArrayBuilder(String key, ApiDefinition.Schema schema,
                              OnReference onReference) implements SchemaBuilder {

        @Override
        public String buildBody() {
            var body = new StringBuilder();
            int outerArraySize = 5;
            ApiDefinition.Schema arrayItem = schema.items();
            precondition("Schema Validation", "Property [%s] array type must contain [items]"
                    .formatted(key), () -> arrayItem != null);

            if (arrayItem.$ref() != null) {
                String k = trimSchemaKeyFromRef(arrayItem.$ref());
                this.onReference.onReferenceFound(k);
                buildArrayValues(key, outerArraySize, body,
                        this.onReference.onReferenceFound(k), onReference);
            } else {
                buildArrayValues(key, outerArraySize, body, arrayItem, onReference);
            }
            return body.toString();
        }
    }

    record SchemaSingleBuilder(String key, ApiDefinition.Schema schema,
                               OnReference onReference) implements SchemaBuilder {

        @Override
        public String buildBody() {
            StringBuilder value = new StringBuilder();
            appendProperty(key, value);
            value.append(buildValue(key, schema, onReference));

            return value.toString();
        }
    }

    // #/components/schemas/Tag
    static String trimSchemaKeyFromRef(String ref) {
        String[] split = ref.split("/");
        return split[split.length - 1];
    }

    static void appendProperty(String key,
                               StringBuilder body) {
        body.append("\"").append(key).append("\":"); // "id:"
    }

    static void buildArrayValues(String key, int outerArraySize, StringBuilder body,
                                 ApiDefinition.Schema arrayItem, OnReference onReference) {
        body.append("[");
        for (int i = 0; i < outerArraySize; i++) {
            body.append(buildValue(key, arrayItem, onReference));

            if (i != outerArraySize - 1) {
                body.append(",");
            }
        }
        body.append("]");
    }


    static Object buildValue(String key, ApiDefinition.Schema schema, OnReference onReference) {
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
                    values.add(buildValue(key, schema.items(), onReference));
                }

                yield values;
            }
            case OBJECT -> new SchemaObjectBuilder(key, schema, onReference).buildBody();
        };
    }
}
