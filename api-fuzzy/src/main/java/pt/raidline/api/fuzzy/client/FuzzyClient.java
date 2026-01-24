package pt.raidline.api.fuzzy.client;

import pt.raidline.api.fuzzy.logging.CLILogger;
import pt.raidline.api.fuzzy.model.AppArguments;
import pt.raidline.api.fuzzy.processors.paths.model.Path;
import pt.raidline.api.fuzzy.processors.paths.model.Path.PathOperation;
import pt.raidline.api.fuzzy.processors.schema.ValueRandomizer;
import pt.raidline.api.fuzzy.processors.schema.component.ComponentBuilder;
import pt.raidline.api.fuzzy.processors.schema.model.SchemaBuilderNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.StructuredTaskScope;
import java.util.function.Function;

import static java.util.concurrent.StructuredTaskScope.Joiner.awaitAllSuccessfulOrThrow;
import static pt.raidline.api.fuzzy.assertions.AssertionUtils.internalAssertion;
import static pt.raidline.api.fuzzy.assertions.AssertionUtils.precondition;
import static pt.raidline.api.fuzzy.processors.schema.ValueRandomizer.StringFormat.fromString;

public class FuzzyClient {

    private static final int NUMBER_OF_CYCLES = Integer.MAX_VALUE;

    private static final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    private final RequestManager manager;
    private final Function<StructuredTaskScope.Configuration, StructuredTaskScope.Configuration> config;
    private final Duration maxTime;

    public FuzzyClient(AppArguments args) {
        this.maxTime = Duration.of(args.maxTime.value(), ChronoUnit.SECONDS);
        Duration timeout = Duration.between(Instant.now(), Instant.now().plus(this.maxTime));
        this.config = conf -> conf
                .withThreadFactory(Thread.ofVirtual()
                        .name("Outer-Loop-Tester-VT", 0)
                        .factory())
                .withTimeout(timeout);

        this.manager = new RequestManager(args.concurrentCallsGate.value(),
                args.concurrentEndpointCalls.value(),
                args.exponentialUserGrowth.value(),
                args.endingCondition.value()
        );
    }

    public void executeRequest(Map<String, SchemaBuilderNode> schemaGraph,
                               List<Path> paths, AppArguments arguments) {

        internalAssertion("Client Preparation",
                () -> arguments.server != null,
                "Server value cannot be value");

        internalAssertion("Client Preparation",
                () -> schemaGraph != null && !schemaGraph.isEmpty(),
                "Schema cannot be null or empty");

        var requestBuilder = HttpRequest.newBuilder()
                .timeout(Duration.ofMinutes(2))
                .header("Content-Type", "application/json");

        var pathsIterator = new PathSupplierIterator(paths, NUMBER_OF_CYCLES);

        while (pathsIterator.hasNext()) {
            var iterator = pathsIterator.next();

            internalAssertion("Client Preparation",
                    () -> iterator != null && iterator.hasNext(),
                    "Paths cannot be null or empty");

            CLILogger.debug("Starting new round of requests");

            try (var scope = StructuredTaskScope.open(awaitAllSuccessfulOrThrow(), config)) {

                do {
                    var path = iterator.next();
                    scope.fork(() -> this.manager.submit(
                            this.createIterator(path, requestBuilder, arguments.server, schemaGraph))
                    );
                } while (iterator.hasNext());

                scope.join();

            } catch (StructuredTaskScope.TimeoutException e) {
                // 4. TIMEOUT REACHED!
                // The scope automatically cancels all running threads here.
                CLILogger.severe("Server shutdown: Max running time of %s exceeded.", maxTime);
                System.exit(0);
            } catch (Exception e) {
                CLILogger.severe("Requests failed: [%s]", e.getMessage());
                System.exit(1);
            }
        }
    }

    private RequestManager.RequestIterator createIterator(Path path, HttpRequest.Builder requestBuilder,
                                                          AppArguments.Arg<String> server,
                                                          Map<String, SchemaBuilderNode> schemaGraph) {
        return new RequestManager.RequestIterator() {
            private int current = 0;

            @Override
            public boolean hasNext() {
                return current < path.operations().size();
            }

            @Override
            public Callable<Void> next(RequestManager.RunContext context) {
                if (current >= path.operations().size()) {
                    throw new NoSuchElementException();
                }
                var operation = path.operations().get(current);
                current++;
                var request = buildRequest(server.value(),
                        context,
                        operation,
                        requestBuilder, path, schemaGraph);
                context.setContext(RequestManager.ContextKey.REQUEST, request);
                CLILogger.debug("Sending request : [%s]", request);
                return () -> {
                    var res = client.send(request, HttpResponse.BodyHandlers.ofString());

                    context.setContext(RequestManager.ContextKey.RESPONSE, res);

                    return null;
                };
            }
        };
    }

    private HttpRequest buildRequest(String basePath, RequestManager.RunContext context,
                                     PathOperation operation, HttpRequest.Builder builder,
                                     Path path, Map<String, SchemaBuilderNode> graph) {

        var uriBuilder = builder
                .uri(URI.create(basePath + resolvePathParams(operation, path.key())));

        var headerBuilder = buildHeaders(uriBuilder, operation);

        return decideHttpMethod(headerBuilder, context, operation, graph).build();
    }

    private HttpRequest.Builder buildHeaders(HttpRequest.Builder builder,
                                             PathOperation operation) {
        if (!operation.opParams().containsKey(Path.ParameterLocation.HEADER)) {
            return builder;
        }

        operation.opParams().get(Path.ParameterLocation.HEADER)
                .forEach(pathParam -> builder.header(pathParam.name(),
                        ValueRandomizer.randomizeStringValue(ValueRandomizer.StringFormat.DEFAULT)));
        // all headers are strings probably, we can assume that, just need to build a random string here
        return builder;
    }

    private HttpRequest.Builder decideHttpMethod(HttpRequest.Builder builder,
                                                 RequestManager.RunContext context,
                                                 PathOperation operation,
                                                 Map<String, SchemaBuilderNode> graph) {
        return switch (operation.op()) {
            case GET -> builder.GET();
            case POST -> {
                String body = buildBody(graph, operation);
                context.setContext(RequestManager.ContextKey.REQUEST, body);
                yield builder.POST(BodyPublishers.ofString(body));
            }
            case PUT -> {
                String body = buildBody(graph, operation);
                context.setContext(RequestManager.ContextKey.REQUEST, body);
                yield builder.PUT(BodyPublishers.ofString(body));
            }
            case DELETE -> builder.DELETE();
        };
    }

    private static String buildBody(Map<String, SchemaBuilderNode> graph, PathOperation operation) {
        return graph.get(
                ComponentBuilder.trimSchemaKeyFromRef(operation.request().ref())).buildSchema(); //todo: me no like this dependency
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
                if (pathParam.schema().type().isInteger()) {
                    return String.valueOf(ValueRandomizer.randomizeIntValue());
                } else {
                    return ValueRandomizer.randomizeStringValue(fromString(pathParam.schema().format()));
                }
            }
        }

        precondition("Path Parameter transformation",
                "Param : [%s] was not found in the schema".formatted(sanitizedParam),
                () -> false);

        return null;
    }
}
