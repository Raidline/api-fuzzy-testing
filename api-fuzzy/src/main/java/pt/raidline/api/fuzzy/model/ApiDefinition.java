package pt.raidline.api.fuzzy.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Represents an OpenAPI 3.0 specification definition.
 */
public record ApiDefinition(
        String openapi,
        Info info,
        List<Tag> tags,
        Map<String, PathItem> paths,
        Components components
) {
    /**
     * Represents the info section of the OpenAPI specification.
     */
    public record Info(String title, String description, String version) {
    }

    /**
     * Represents a tag in the OpenAPI specification.
     */
    public record Tag(String name, String description) {
    }

    /**
     * Represents a path item containing HTTP method operations.
     */
    public record PathItem(Operation get,
                           Operation post,
                           Operation put,
                           Operation delete) {

        public HttpOperation getOperation() {
            if (get != null) {
                return HttpOperation.GET;
            }

            if (post != null) {
                return HttpOperation.POST;
            }

            if (put != null) {
                return HttpOperation.PUT;
            }

            return HttpOperation.DELETE;
        }
    }

    /**
     * Represents an HTTP operation (endpoint).
     */
    public record Operation(
            List<String> tags,
            String summary,
            String description,
            String operationId,
            List<Parameter> parameters,
            RequestBody requestBody,
            Map<String, Response> responses
    ) {
    }

    /**
     * Represents a parameter for an operation.
     */
    public record Parameter(
            String name,
            String in, // "query", "header", "path", "cookie"
            String description,
            boolean required,
            Schema schema
    ) {
    }

    /**
     * Represents a request body.
     */
    public record RequestBody(
            String description,
            Map<String, MediaType> content,
            boolean required
    ) {
    }

    /**
     * Represents a response.
     */
    public record Response(
            String description,
            Map<String, MediaType> content
    ) {
    }

    /**
     * Represents a media type (e.g., application/json).
     */
    public record MediaType(Schema schema) {
    }

    /**
     * Represents a schema definition.
     */
    public record Schema(
            SchemaType type,
            String format,
            String pattern,
            String $ref,
            String description,
            String example,
            List<String> required,
            Map<String, Schema> properties,
            Schema items,
            Schema additionalProperties,
            @JsonProperty("enum")
            List<String> enumValues, // "enum" in JSON, renamed to avoid Java keyword
            Integer minimum,
            Integer maximum,
            Integer minLength,
            Integer maxLength,
            Integer minItems,
            Integer maxItems,
            List<Schema> allOf,
            List<Schema> oneOf,
            List<Schema> anyOf
    ) {
    }

    /**
     * Represents the components section containing reusable schemas.
     */
    public record Components(Map<String, Schema> schemas) {
    }

    public enum SchemaType {
        STRING, INTEGER, ARRAY, OBJECT, BOOLEAN;

        @JsonCreator
        public static SchemaType fromString(String type) {
            return Arrays.stream(values())
                    .filter(t -> t.name().toLowerCase().equalsIgnoreCase(type))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Argument [%s] is not recognizable"
                            .formatted(type)));
        }

        public boolean isString() {
            return this == STRING;
        }

        public boolean isInteger() {
            return this == INTEGER;
        }

        public boolean isArray() {
            return this == ARRAY;
        }

        public boolean isBoolean() {
            return this == BOOLEAN;
        }

        public boolean isObject() {
            return this == OBJECT;
        }
    }
}
