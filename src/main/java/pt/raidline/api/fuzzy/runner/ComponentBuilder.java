package pt.raidline.api.fuzzy.runner;

import pt.raidline.api.fuzzy.logging.CLILogger;
import pt.raidline.api.fuzzy.model.ApiDefinition;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import static pt.raidline.api.fuzzy.assertions.AssertionUtils.precondition;

class ComponentBuilder {
    //todo: this map will probably return a well defined interface, where we can get (body, http method, http header,
    // etc..)
    private final Map<String, Supplier<String>> schemaPool = new HashMap<>();

    //todo: we can have cross-references between schemas

    //todo: probably i need to make unit tests for this
    Supplier<String> preBuild(String key, ApiDefinition.Schema schema) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(schema);

        precondition("Multiple Schema Creation",
                "You are trying to build multiple schema for the same key : [%s]"
                        .formatted(key),
                () -> !schemaPool.containsKey(key));

        this.schemaPool.computeIfAbsent(key, k ->
                () -> new SchemaBuilderImpl(k, schema).buildBody());

        return schemaPool.get(key);
    }

    //this probable will be something for complex, that can return body, http method, headers, etc..
    //the idea is that each one of this instances can create the needed values to make the call
    //another part will deal with the requests and all of that
    private interface SchemaBuilder {
        String buildBody();
    }

    //todo: this needs to be much more complex, here we will build all the random shit!
    //for example send without required fields, send with unknown fields, send random values
    private record SchemaBuilderImpl(String key, ApiDefinition.Schema schema) implements SchemaBuilder {

        @Override
        public String buildBody() {
            //CLILogger.debug("Building for schema : [%s]", schema);
            if (isArray(schema)) {
                CLILogger.debug("Building an array");

                //deal with this later
            }

            // the body does not need to well-formed
            var body = new StringBuilder("{\n");

            for (var entry : schema.properties().entrySet()) {
                ApiDefinition.Schema value = entry.getValue();

                if (value.$ref() != null) {
                    CLILogger.warn("We have a cross-reference, skip for now : [%s]", entry.getKey());

                    continue;
                }

                if (isArray(value)) {
                    ApiDefinition.Schema arrayItem = value.items();
                    precondition("Schema Validation", "Property [%s] array type must contain [items]"
                            .formatted(entry.getKey()), () -> arrayItem != null);

                    if (arrayItem.$ref() != null) {
                        CLILogger.warn("We have a cross-reference inside the array, skip for now : [%s]",
                                arrayItem.$ref());
                    } else {
                        int size = 5;
                        appendProperty(entry.getKey(), body);
                        body.append("[");
                        for (int i = 0; i < size; i++) {
                            body.append(buildValue(arrayItem));

                            if (i != size - 1) {
                                body.append(",");
                            }
                        }
                        body.append("]");
                    }
                } else {
                    appendProperty(entry.getKey(), body);
                    body.append(buildValue(value)); //{value}
                }

                body.append(",").append("\n");
            }
            body.deleteCharAt(body.length() - 2); //remove last ","


            return body.append("}").toString();
        }

        private static void appendProperty(String key,
                                           StringBuilder body) {
            body.append("\"").append(key).append("\":"); // "id:"
        }

        private boolean isArray(ApiDefinition.Schema schema) {
            return "array".equalsIgnoreCase(schema.type());
        }


        private Object buildValue(ApiDefinition.Schema schema) {
            if ("integer".equalsIgnoreCase(schema.type())) {
                return 1;
            }

            if ("date-time".equalsIgnoreCase(schema.format())) {
                return LocalDateTime.now().toString();
            }

            return "some string";
        }
    }
}
