package pt.raidline.api.fuzzy.model;

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
            String type,
            String format,
            String pattern,
            String $ref,
            String description,
            String example,
            List<String> required,
            Map<String, Schema> properties,
            Schema items,
            Schema additionalProperties,
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
}
