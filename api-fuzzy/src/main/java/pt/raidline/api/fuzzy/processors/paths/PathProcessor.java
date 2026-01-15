package pt.raidline.api.fuzzy.processors.paths;

import pt.raidline.api.fuzzy.model.ApiDefinition;
import pt.raidline.api.fuzzy.model.HttpOperation;
import pt.raidline.api.fuzzy.processors.paths.model.Path;
import pt.raidline.api.fuzzy.processors.paths.model.Path.ParameterLocation;
import pt.raidline.api.fuzzy.processors.paths.model.Path.PathOperation;
import pt.raidline.api.fuzzy.processors.paths.model.Path.PathParameter;

import java.util.*;

import static pt.raidline.api.fuzzy.assertions.AssertionUtils.*;

public class PathProcessor {

    public Iterator<Path> processPaths(Map<String, ApiDefinition.PathItem> paths) {
        precondition("Path Processing",
                "Received [null] for the paths inside the ApiDefinition ",
                () -> paths == null);

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
            ops.add(processOperation(operation, operation.fromItemToOp.apply(value)));
        }

        return ops;
    }

    private PathOperation processOperation(HttpOperation httpOperation,
                                           ApiDefinition.Operation operation) {
        internalAssertion("Get operation from ENUM",
                () -> operation != null,
                "Operation function in enum [%s] method should not return null."
                        .formatted(httpOperation.name()));

        var params = new HashMap<ParameterLocation, List<PathParameter>>();
        if (operation.parameters() != null) {
            params = calculateParams(operation.parameters());
        }

        Path.OperationExchange request = null;
        if (operation.requestBody() != null) {
            request = extractRequest(operation.requestBody());
        }

        Path.OperationExchange successResponse = null;
        var errorResponses = new HashMap<Integer, Path.OperationExchange>();
        if (operation.responses() != null) {
            successResponse = extractSuccessResponse(operation.responses());
            errorResponses = extractErrorResponses(operation.responses());
        }


        return new PathOperation(httpOperation, params, successResponse, errorResponses, request);
    }

    private Path.OperationExchange extractSuccessResponse(Map<String, ApiDefinition.Response> responses) {
        internalAssertion("Calculate Request Body",
                () -> responses != null,
                "Responses from a schema should not be null");

        precondition("Calculate Request Body",
                "Schema should contain a successful response",
                () -> responses.containsKey("200") || responses.containsKey("201")
                        || responses.containsKey("202") || responses.containsKey("204"));
        //there can only be one schema for the request, but we do not know the content beforehand
        // that is why we have a map with 1 value

        //todo: do the other http status, each have a different way of giving responses
        var responseBody = responses.get("200");

        internalAssertion("Response Body",
                () -> responseBody.content().size() == 1,
                "Content should only have 1 possible schema");

        var exchange = new Path.OperationExchange[1];
        for (var mediaTypeEntry : responseBody.content().entrySet()) {
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

    private HashMap<Integer, Path.OperationExchange> extractErrorResponses(Map<String, ApiDefinition.Response> responses) {
        internalAssertion("Calculate Request Body",
                () -> responses != null,
                "Responses from a schema should not be null");

        precondition("Calculate Request Body",
                "Schema should contain an error response",
                () -> contains5xxErrors(responses) || contains4xxErrors(responses));

        return null;
    }

    private boolean contains5xxErrors(Map<String, ApiDefinition.Response> responses) {
        return responses.containsKey("500") || responses.containsKey("501") || responses.containsKey("502")
                || responses.containsKey("503") || responses.containsKey("504");
    }

    private boolean contains4xxErrors(Map<String, ApiDefinition.Response> responses) {
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

    private static HashMap<ParameterLocation, List<PathParameter>> calculateParams(List<ApiDefinition.Parameter> parameters) {
        internalAssertion("Calculate Params",
                () -> parameters != null,
                "Parameters from a schema should not be null");

        var opParams = new HashMap<ParameterLocation,
                List<PathParameter>>(parameters.size());

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
