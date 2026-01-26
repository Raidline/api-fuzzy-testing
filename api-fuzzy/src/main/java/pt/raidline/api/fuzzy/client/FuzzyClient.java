package pt.raidline.api.fuzzy.client;

import pt.raidline.api.fuzzy.TestController;
import pt.raidline.api.fuzzy.client.model.RunContext;
import pt.raidline.api.fuzzy.logging.CLILogger;
import pt.raidline.api.fuzzy.model.ApiDefinition;
import pt.raidline.api.fuzzy.model.AppArguments;
import pt.raidline.api.fuzzy.processors.paths.model.Path;
import pt.raidline.api.fuzzy.processors.paths.model.Path.PathOperation;
import pt.raidline.api.fuzzy.processors.schema.ValueRandomizer;
import pt.raidline.api.fuzzy.processors.schema.ValueRandomizer.StringFormat;
import pt.raidline.api.fuzzy.processors.schema.component.ComponentBuilder;
import pt.raidline.api.fuzzy.processors.schema.model.SchemaBuilderNode;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import static pt.raidline.api.fuzzy.assertions.AssertionUtils.internalAssertion;
import static pt.raidline.api.fuzzy.assertions.AssertionUtils.precondition;
import static pt.raidline.api.fuzzy.processors.paths.model.Path.ParameterLocation.HEADER;
import static pt.raidline.api.fuzzy.processors.paths.model.Path.ParameterLocation.PATH;
import static pt.raidline.api.fuzzy.processors.paths.model.Path.ParameterLocation.QUERY;
import static pt.raidline.api.fuzzy.processors.schema.ValueRandomizer.StringFormat.fromString;

public class FuzzyClient {

    private static final int NUMBER_OF_CYCLES = Integer.MAX_VALUE;

    private final RequestExecutor manager;
    private final TestController controller;
    private final AtomicInteger run;
    private final Duration maxTime;

    public FuzzyClient(AppArguments args, TestController controller) {
        this.maxTime = Duration.of(args.maxTime.value(), ChronoUnit.SECONDS);
        this.controller = controller;
        this.run = new AtomicInteger(0);
        this.manager = new RequestExecutor(controller, args.concurrentCallsGate.value());
    }

    public void executeRequest(Map<String, SchemaBuilderNode> schemaGraph,
                               List<Path> paths, AppArguments arguments) {

        internalAssertion("Client Preparation",
                () -> arguments.server != null,
                "Server value cannot be value");

        internalAssertion("Client Preparation",
                () -> schemaGraph != null && !schemaGraph.isEmpty(),
                "Schema cannot be null or empty");

        var pathsIterator = new PathSupplierIterator(paths, NUMBER_OF_CYCLES);

        Instant now = Instant.now();

        var timeout = Duration.between(now, now.plus(maxTime));

        controller.startTimeoutCheck();

        CLILogger.info("Starting making requests");
        while (pathsIterator.hasNext()) {
            if (controller.shouldStop() || controller.timeoutReached) break;

            var iterator = pathsIterator.next();

            internalAssertion("Client Preparation",
                    () -> iterator != null && iterator.hasNext(),
                    "Paths cannot be null or empty");

            CLILogger.debug("Starting new round of requests");

            var context = new RunContext(run.incrementAndGet());

            while (iterator.hasNext()) {
                if (controller.shouldStop() || controller.timeoutReached) break;
                var path = iterator.next();

                RequestIterator calls = this.createIterator(path, arguments.server, schemaGraph);
                if (!this.manager.submit(calls, context, timeout)) {
                    return;
                }
            }
        }
    }

    private RequestIterator createIterator(Path path,
                                           AppArguments.Arg<String> server,
                                           Map<String, SchemaBuilderNode> schemaGraph) {
        return new RequestIterator() {
            private final AtomicInteger current = new AtomicInteger(0);

            @Override
            public boolean hasNext() {
                return current.get() < path.operations().size();
            }

            @Override
            public HttpRequest next(RunContext context) {
                if (current.get() >= path.operations().size()) {
                    throw new NoSuchElementException();
                }

                var requestBuilder = HttpRequest.newBuilder()
                        .header("Content-Type", "application/json");

                var operation = path.operations().get(current.getAndIncrement());
                var request = buildRequest(server.value(),
                        context,
                        operation,
                        requestBuilder, path, schemaGraph).build();
                context.setContext(RunContext.ContextKey.REQUEST, request);

                return request;
            }
        };
    }

    private HttpRequest.Builder buildRequest(String basePath, RunContext context,
                                             PathOperation operation, HttpRequest.Builder builder,
                                             Path path, Map<String, SchemaBuilderNode> graph) {


        var uri = new StringBuilder(basePath)
                .append(resolvePathParams(operation, path.key()));

        createQueries(operation.opParams(), uri);

        var uriBuilder = builder.uri(URI.create(uri.toString()));

        var headerBuilder = buildHeaders(uriBuilder, operation);

        return decideHttpMethod(headerBuilder, context, operation, graph);
    }

    private void createQueries(Map<Path.ParameterLocation, List<Path.PathParameter>> params,
                               StringBuilder uriBuilder) {

        if (!params.containsKey(QUERY)) {
            return;
        }

        var queries = params.get(QUERY);

        uriBuilder.append("?");

        for (Path.PathParameter query : queries) {
            var schema = query.schema();
            uriBuilder.append(query.name())
                    .append("=");
            if (schema.type().isInteger()) {
                uriBuilder.append(ValueRandomizer.randomizeIntValue(schema.minimum(), schema.maximum()));
            } else {
                uriBuilder.append(ValueRandomizer.randomizeStringValue(StringFormat.URI,
                        schema.enumValues(), schema.minLength(),
                        schema.maxLength(), schema.pattern()));
            }
            uriBuilder.append("&");
        }

        uriBuilder.deleteCharAt(uriBuilder.length() - 1); //delete last &
    }

    private HttpRequest.Builder buildHeaders(HttpRequest.Builder builder,
                                             PathOperation operation) {
        if (!operation.opParams().containsKey(HEADER)) {
            return builder;
        }

        operation.opParams().get(HEADER)
                .forEach(pathParam -> {
                    ApiDefinition.Schema schema = pathParam.schema();
                    builder.header(pathParam.name(),
                            ValueRandomizer.randomizeStringValue(StringFormat.DEFAULT,
                                    schema.enumValues(), schema.minLength(),
                                    schema.maxLength(), schema.pattern()));
                });
        return builder;
    }

    private HttpRequest.Builder decideHttpMethod(HttpRequest.Builder builder,
                                                 RunContext context,
                                                 PathOperation operation,
                                                 Map<String, SchemaBuilderNode> graph) {
        return switch (operation.op()) {
            case GET -> builder.GET();
            case POST -> {
                String body = buildBody(graph, operation);
                context.setContext(RunContext.ContextKey.REQUEST, body);
                yield builder.POST(BodyPublishers.ofString(body));
            }
            case PUT -> {
                String body = buildBody(graph, operation);
                context.setContext(RunContext.ContextKey.REQUEST, body);
                yield builder.PUT(BodyPublishers.ofString(body));
            }
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

    private static String transformParam(Map<Path.ParameterLocation, List<Path.PathParameter>> uriParams,
                                         String param) {
        precondition("Path Parameter transformation",
                "Found an '{' and we have no params!",
                () -> uriParams.containsKey(PATH));

        var sanitizedParam = param.substring(1, param.length() - 1);

        var pathParams = uriParams.get(PATH);

        for (Path.PathParameter pathParam : pathParams) {
            if (pathParam.name().equalsIgnoreCase(sanitizedParam)) {
                ApiDefinition.Schema schema = pathParam.schema();

                if (schema.type().isInteger()) {
                    return String.valueOf(ValueRandomizer.randomizeIntValue(schema.minimum(), schema.maximum()));
                } else {
                    return ValueRandomizer.randomizeStringValue(
                            fromString(schema.format()),
                            schema.enumValues(),
                            schema.minLength(), schema.maxLength(),
                            schema.pattern());
                }
            }
        }

        precondition("Path Parameter transformation",
                "Param : [%s] was not found in the schema".formatted(sanitizedParam),
                () -> false);

        return null;
    }

    interface RequestIterator {

        boolean hasNext();

        HttpRequest next(RunContext context);
    }
}
