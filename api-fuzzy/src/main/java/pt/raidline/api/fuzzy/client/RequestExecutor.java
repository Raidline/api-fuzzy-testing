package pt.raidline.api.fuzzy.client;

import pt.raidline.api.fuzzy.TestController;
import pt.raidline.api.fuzzy.client.model.RunContext;
import pt.raidline.api.fuzzy.logging.CLILogger;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.concurrent.StructuredTaskScope.Joiner.awaitAllSuccessfulOrThrow;
import static pt.raidline.api.fuzzy.assertions.AssertionUtils.internalAssertion;

public class RequestExecutor {

    private static final ThreadFactory threadFactory = Thread.ofVirtual()
            .name("Fuzzy-Tester-VT", 0)
            .factory();

    private static final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    private static final Function<RunContext, String> templateTerminationErrorMessage = context -> """
            There as been an error on run : [%d]\s
             \
            while doing task nr [%d]\s
            Associated with Path : [%s]\s
             \
            Request Body : [%s]\s
            Headers : [%s]\s
            Response Body : status - [%d], body : [%s]"""
            .formatted(context.run, context.innerRun,
                    context.getHttpRequest().map(req -> req.uri().getPath()).orElse("No Path"),
                    context.getHttpRequestBody().orElse("No Body"),
                    context.getHttpRequest().map(req -> req.headers().map())
                            .map(m -> m.entrySet()
                                    .stream()
                                    .map(entry -> entry.getKey() + ":" + String.join(",", entry.getValue()))
                                    .collect(Collectors.joining(";")))
                            .orElse("No headers"),
                    context.getResponseContext().map(HttpResponse::statusCode).orElse(1),
                    context.getResponseContext().map(HttpResponse::body).orElse("Error"));

    private static final Function<StructuredTaskScope.Configuration, StructuredTaskScope.Configuration> config =
            conf -> conf.withThreadFactory(threadFactory);

    private final TestController controller;
    private final Semaphore callsThrottled;
    private final AtomicInteger innerRun;

    RequestExecutor(TestController controller, int concurrentCallsGate) {
        this.controller = controller;
        this.callsThrottled = new Semaphore(concurrentCallsGate);
        this.innerRun = new AtomicInteger(0);
    }

    boolean submit(FuzzyClient.RequestIterator calls,
                   RunContext context,
                   Duration timeout) {
        Objects.requireNonNull(calls);
        internalAssertion("Submit Requests", calls::hasNext,
                "You cannot submit an empty iterator for tasks");

        try (var scope = StructuredTaskScope.open(awaitAllSuccessfulOrThrow(),
                config.andThen(c -> c.withTimeout(timeout)))) {
            do {
                if (controller.shouldStop() || controller.timeoutReached) break;

                var request = calls.next(context);

                context.innerRun = this.innerRun.incrementAndGet();
                try {
                    callsThrottled.acquire();
                    scope.fork(() -> {
                        if (controller.shouldStop() || controller.timeoutReached) return null;
                        CLILogger.info("Sending request : [%s]", request);
                        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

                        context.setContext(RunContext.ContextKey.RESPONSE, response);
                        CLILogger.debug("Response from server : [%d-%s]",
                                response.statusCode(), response.body());

                        return null;
                    });
                } finally {
                    callsThrottled.release();
                }
            } while (calls.hasNext());

            scope.join();

            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            this.onFailure(e, context);
        } catch (StructuredTaskScope.TimeoutException e) {
            CLILogger.severe("Server shutdown: Max running time of %s exceeded.", timeout.getSeconds());
        } catch (Exception e) {
            this.onFailure(e, context);
        }

        return false;
    }

    private <E extends Throwable> void onFailure(E failure, RunContext context) {
        Objects.requireNonNull(failure);
        var message = cleanMessage(failure);
        CLILogger.severe("Requests failed: [%s]-[%s]", failure.getClass().getName(), message);
        CLILogger.severe("Stacktrace:\n");
        for (StackTraceElement stackTraceElement : failure.getStackTrace()) {
            CLILogger.severe(stackTraceElement.toString());
        }

        CLILogger.severe(templateTerminationErrorMessage.apply(context));
    }

    private <E extends Throwable> String cleanMessage(E failure) {
        return failure.getMessage() != null ? failure.getMessage() : "Error occurred";
    }
}
