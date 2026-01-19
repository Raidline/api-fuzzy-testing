package pt.raidline.api.fuzzy.client;

import pt.raidline.api.fuzzy.custom.AsyncQueue;
import pt.raidline.api.fuzzy.custom.PathSupplierIterator;
import pt.raidline.api.fuzzy.logging.CLILogger;
import pt.raidline.api.fuzzy.model.AppArguments;
import pt.raidline.api.fuzzy.processors.paths.model.Path;
import pt.raidline.api.fuzzy.processors.paths.model.Path.PathOperation;
import pt.raidline.api.fuzzy.processors.schema.component.ComponentBuilder;
import pt.raidline.api.fuzzy.processors.schema.model.SchemaBuilderNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static pt.raidline.api.fuzzy.assertions.AssertionUtils.internalAssertion;
import static pt.raidline.api.fuzzy.assertions.AssertionUtils.precondition;

public class FuzzyClient {

    private static final int NUMBER_OF_CYCLES = 5;

    private static final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public void executeRequest(Map<String, SchemaBuilderNode> schemaGraph,
                               List<Path> paths, AppArguments.Arg server) {

        internalAssertion("Client Preparation",
                () -> schemaGraph != null && !schemaGraph.isEmpty(),
                "Schema cannot be null or empty");

        var requestBuilder = HttpRequest.newBuilder()
                .timeout(Duration.ofMinutes(2))
                .header("Content-Type", "application/json");

        // default value to try and not allocate memory while sending the requests
        var pathsIterator = new PathSupplierIterator(paths, NUMBER_OF_CYCLES);
        var queue = new AsyncQueue(16);

        do {
            var iterator = pathsIterator.next();

            internalAssertion("Client Preparation",
                    () -> iterator != null && iterator.hasNext(),
                    "Paths cannot be null or empty");

            CLILogger.debug("Starting new round of requests");

            do {
                var path = iterator.next();
                for (PathOperation operation : path.operations()) {
                    var request = buildRequest(
                            server.value(),
                            operation,
                            requestBuilder,
                            path, schemaGraph
                    );
                    CLILogger.debug("Sending request : [%s]", request);

                    Supplier<CompletableFuture<Void>> action = () -> client.sendAsync(request,
                                    HttpResponse.BodyHandlers.ofString())
                            .thenAccept(res ->
                                    CLILogger.info("Response from server : [%d-%s]", res.statusCode(), res.body()));

                    while (!queue.enqueue(action)) {
                        Thread.yield(); // give the room to another thread
                    }

                }
            } while (iterator.hasNext());
        } while (pathsIterator.hasNext());

        queue.syncAll();
    }

    private HttpRequest buildRequest(String basePath, PathOperation operation, HttpRequest.Builder builder,
                                     Path path, Map<String, SchemaBuilderNode> graph) {

        var uriBuilder = builder
                .uri(URI.create(basePath + resolvePathParams(operation, path.key())));

        var headerBuilder = buildHeaders(uriBuilder, operation);

        return decideHttpMethod(headerBuilder, operation, graph).build();
    }

    private HttpRequest.Builder buildHeaders(HttpRequest.Builder builder,
                                             PathOperation operation) {
        if (!operation.opParams().containsKey(Path.ParameterLocation.HEADER)) {
            return builder;
        }

        operation.opParams().get(Path.ParameterLocation.HEADER)
                .forEach(pathParam -> builder.header(pathParam.name(), "213"));
        // all headers are strings probably, we can assume that, just need to build a random string here
        return builder;
    }

    private HttpRequest.Builder decideHttpMethod(HttpRequest.Builder builder,
                                                 PathOperation operation,
                                                 Map<String, SchemaBuilderNode> graph) {
        return switch (operation.op()) {
            case GET -> builder.GET();
            case POST -> builder.POST(BodyPublishers.ofString(buildBody(graph, operation)));
            case PUT -> builder.PUT(BodyPublishers.ofString(buildBody(graph, operation)));
            case DELETE -> builder.DELETE();
        };
    }

    private static String buildBody(Map<String, SchemaBuilderNode> graph, PathOperation operation) {
        return graph.get(
                ComponentBuilder.trimSchemaKeyFromRef(operation.request().ref())).buildSchema();
    }

    private static String resolvePathParams(PathOperation postOp, String path) {
        StringBuilder newPath = new StringBuilder("/");
        var parts = path.split("/");

        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (part.contains("{")) {
                newPath.append(transformParam(postOp.opParams(), part))
                        .append("/");
            } else {
                newPath.append(part).append("/");
            }
        }

        return newPath.deleteCharAt(newPath.length() - 1).toString();
    }

    //todo: we can improve this by making this into a map
    private static String transformParam(Map<Path.ParameterLocation, List<Path.PathParameter>> uriParams,
                                         String param) {
        precondition("Path Parameter transformation",
                "Found an '{' and we have no params!",
                () -> uriParams.containsKey(Path.ParameterLocation.PATH));

        var sanitizedParam = param.substring(1, param.length() - 1);

        var pathParams = uriParams.get(Path.ParameterLocation.PATH);

        for (Path.PathParameter pathParam : pathParams) {
            if (pathParam.name().equalsIgnoreCase(sanitizedParam)) {
                return "123";
            }
        }

        precondition("Path Parameter transformation",
                "Param : [%s] was not found in the schema".formatted(sanitizedParam),
                () -> false);

        return null;
    }
}
