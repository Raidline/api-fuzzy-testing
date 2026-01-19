package pt.raidline.api.fuzzy.processors.paths;

import pt.raidline.api.fuzzy.model.ApiDefinition;
import pt.raidline.api.fuzzy.model.HttpOperation;
import pt.raidline.api.fuzzy.processors.paths.model.Path;
import pt.raidline.api.fuzzy.processors.paths.model.Path.ParameterLocation;
import pt.raidline.api.fuzzy.processors.paths.model.Path.PathOperation;
import pt.raidline.api.fuzzy.processors.paths.model.Path.PathParameter;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static pt.raidline.api.fuzzy.assertions.AssertionUtils.internalAssertion;
import static pt.raidline.api.fuzzy.assertions.AssertionUtils.precondition;

public class PathProcessor {

    public Iterator<Path> processPaths(Map<String, ApiDefinition.PathItem> paths) {
        precondition("Path Processing",
                "Received [null] for the paths inside the ApiDefinition ",
                () -> paths != null);

        var pathsToOperations = new ArrayList<Path>(paths.size());
        for (var entry : paths.entrySet()) {
            pathsToOperations.add(
                    new Path(
                            entry.getKey(),
                            extractOperations(entry.getValue())
                    )
            );
        }

        //todo: we could (if justified) return a custom iterator
        return pathsToOperations.iterator();
    }

    private List<PathOperation> extractOperations(ApiDefinition.PathItem value) {
        precondition("Operation Path Item",
                "We are trying to extract operations from a null PathItem",
                () -> value != null);

        var ops = new ArrayList<PathOperation>(HttpOperation.values().length);

        for (HttpOperation operation : HttpOperation.values()) {
            var pathOperation = operation.fromItemToOp.apply(value);
            if (pathOperation != null) {
                ops.add(processOperation(operation, pathOperation));
            }
        }

        return ops;
    }

    private PathOperation processOperation(HttpOperation httpOperation,
                                           ApiDefinition.Operation operation) {
        internalAssertion("Get operation from ENUM",
                () -> operation != null,
                "Operation function in enum [%s] method should not return null."
                        .formatted(httpOperation.name()));

        var params = new EnumMap<ParameterLocation, List<PathParameter>>(ParameterLocation.class);
        if (operation.parameters() != null) {
            params = calculateParams(operation.parameters());
        }

        Path.OperationExchange request = null;
        if (operation.requestBody() != null) {
            request = extractRequest(operation.requestBody());
        }

        var result = digestResponses(operation, operation.responses());
        var successResponses = result.getOrDefault(ResponseType.SUCCESS, Map.of());
        var errorResponses = result.getOrDefault(ResponseType.ERROR, Map.of());

        return new PathOperation(httpOperation, params, successResponses, errorResponses, request);
    }

    // Define a temporary key for grouping
    private enum ResponseType {SUCCESS, ERROR, IGNORED}

    private Map<ResponseType, Map<Integer, Path.OperationExchange>> digestResponses(ApiDefinition.Operation operation,
                                                                                    Map<String, ApiDefinition.Response> responses) {
        internalAssertion("Calculate Request Body",
                () -> responses != null,
                "Responses from a schema should not be null");

        precondition("Calculate Request Body",
                "Operation [%s], Schema [%s] should contain responses".formatted(operation.getSanitizedDescription(),
                        String.join(",", responses.keySet())),
                () -> containsSuccessResponses(responses) || containsErrorResponses(responses));


        return responses.entrySet().stream()
                .collect(Collectors.groupingBy(
                        entry -> {
                            int code = Integer.parseInt(entry.getKey());
                            if (code >= 200 && code < 300) return ResponseType.SUCCESS;
                            if (code >= 400) return ResponseType.ERROR;
                            return ResponseType.IGNORED;
                        },
                        // Downstream collector: Transform the entries into the final Map format
                        Collectors.toMap(
                                entry -> Integer.parseInt(entry.getKey()),
                                entry -> extractResponse(entry.getKey(), entry.getValue())
                        )
                ));
    }

    private static boolean containsSuccessResponses(Map<String, ApiDefinition.Response> responses) {
        return responses.containsKey("200") || responses.containsKey("201")
                || responses.containsKey("202") || responses.containsKey("204");
    }

    private static boolean containsErrorResponses(Map<String, ApiDefinition.Response> responses) {
        return contains5xxErrors(responses) || contains4xxErrors(responses);
    }

    private static boolean contains5xxErrors(Map<String, ApiDefinition.Response> responses) {
        return responses.containsKey("500") || responses.containsKey("501") || responses.containsKey("502")
                || responses.containsKey("503") || responses.containsKey("504");
    }

    private static boolean contains4xxErrors(Map<String, ApiDefinition.Response> responses) {
        return responses.containsKey("400") || responses.containsKey("401") || responses.containsKey("402")
                || responses.containsKey("403") || responses.containsKey("404") || responses.containsKey("412");
    }

    private Path.OperationExchange extractRequest(ApiDefinition.RequestBody requestBody) {
        internalAssertion("Calculate Request Body",
                () -> requestBody != null,
                "Request from a schema should not be null");

        //there can only be one schema for the request, but we do not know the content beforehand
        // that is why we have a map with 1 value

        internalAssertion("Request Body",
                () -> requestBody.content().size() == 1,
                "Content should only have 1 possible schema");

        var exchange = new Path.OperationExchange[1];
        for (var mediaTypeEntry : requestBody.content().entrySet()) {
            var schema = mediaTypeEntry.getValue().schema();
            internalAssertion("Request Body",
                    () -> schema != null,
                    "The schema of the request cannot be null");

            String ref;
            ApiDefinition.SchemaType type;
            if (schema.type() != null && schema.type().isArray()) {
                internalAssertion("Request Body",
                        () -> schema.items() != null,
                        "The items of an array schema of the request cannot be null");
                ref = schema.items().$ref();
                type = ApiDefinition.SchemaType.ARRAY;
            } else {
                internalAssertion("Request Body",
                        () -> schema.$ref() != null,
                        "The ref an object schema of the request cannot be null");
                ref = schema.$ref();
                type = ApiDefinition.SchemaType.OBJECT;
            }

            exchange[0] = new Path.OperationExchange(
                    mediaTypeEntry.getKey(),
                    type,
                    ref
            );
        }

        return exchange[0];
    }

    private Path.OperationExchange extractResponse(String key, ApiDefinition.Response responseBody) {
        if ("204".equalsIgnoreCase(key)) {
            return new Path.OperationExchange(null, null, null);
        }

        internalAssertion("Response Body",
                () -> responseBody.content() != null && !"204".equalsIgnoreCase(key),
                "Response should have a content : [%s]".formatted(responseBody.toString()));


        internalAssertion("Response Body",
                () -> responseBody.content().size() == 1,
                "Content should only have 1 possible schema");

        var exchange = new Path.OperationExchange[1];
        for (var mediaTypeEntry : responseBody.content().entrySet()) {
            var schema = mediaTypeEntry.getValue().schema();
            internalAssertion("Response Body",
                    () -> schema != null,
                    "The schema of the response cannot be null");

            String ref;
            ApiDefinition.SchemaType type;
            if (schema.type() != null && schema.type().isArray()) {
                internalAssertion("Response Body",
                        () -> schema.items() != null,
                        "The items of an array schema of the response cannot be null");
                ref = schema.items().$ref();
                type = ApiDefinition.SchemaType.ARRAY;
            } else {
                internalAssertion("Response Body",
                        () -> schema.$ref() != null,
                        "The ref an object schema of the response cannot be null");
                ref = schema.$ref();
                type = ApiDefinition.SchemaType.OBJECT;
            }

            exchange[0] = new Path.OperationExchange(
                    mediaTypeEntry.getKey(),
                    type,
                    ref
            );
        }

        return exchange[0];
    }

    private static EnumMap<ParameterLocation, List<PathParameter>> calculateParams(List<ApiDefinition.Parameter> parameters) {
        internalAssertion("Calculate Params",
                () -> parameters != null,
                "Parameters from a schema should not be null");

        var opParams = new EnumMap<ParameterLocation, List<PathParameter>>(ParameterLocation.class);

        for (ApiDefinition.Parameter parameter : parameters) {

            var location = ParameterLocation.fromString(parameter.in());

            opParams.compute(location, (k, v) -> {
                if (v == null) {
                    ArrayList<PathParameter> p = new ArrayList<>();
                    p.add(getParameter(parameter));
                    return p;
                }

                v.add(getParameter(parameter));

                return v;
            });
        }

        return opParams;
    }

    private static PathParameter getParameter(ApiDefinition.Parameter parameter) {
        return new PathParameter(
                parameter.name(),
                parameter.schema()
        );
    }

}
