package pt.raidline.api.fuzzy.client;

import pt.raidline.api.fuzzy.logging.CLILogger;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.concurrent.StructuredTaskScope.Joiner.awaitAllSuccessfulOrThrow;
import static pt.raidline.api.fuzzy.assertions.AssertionUtils.internalAssertion;

public class RequestManager {

    private static final ThreadFactory threadFactory = Thread.ofVirtual()
            .name("Fuzzy-Tester-VT", 0)
            .factory();
    private static final Function<StructuredTaskScope.Configuration, StructuredTaskScope.Configuration> taskScopeConfiguration =
            config -> config.withThreadFactory(threadFactory);

    private static final Function<RunContext, String> templateTerminationErrorMessage = context -> """
            There as been an error on run : [%d]\s
             \
            while doing task nr [%d]\s
            Associated with Path : [%s]\s
             \
            Request Body : [%s]\s
            Response Body : status - [%d], body : [%s]"""
            .formatted(context.run, context.innerRun,
                    context.getHttpRequest().map(req -> req.uri().getPath()).orElse("No Path"),
                    context.getHttpRequestBody().orElse("No Body"),
                    context.getResponseContext().map(HttpResponse::statusCode).orElse(1),
                    context.getResponseContext().map(HttpResponse::body).orElse("Error"));

    private final Semaphore gate;
    private int run;
    private final AtomicInteger innerRun;

    RequestManager(int concurrencyGate) {
        this.gate = new Semaphore(concurrencyGate);
        this.run = 0;
        this.innerRun = new AtomicInteger(0);
    }

    void submit(RequestIterator calls) {
        Objects.requireNonNull(calls);
        internalAssertion("Submit Requests", calls::hasNext,
                "You cannot submit an empty iterator for tasks");
        run++;

        var context = new RunContext(this.run);
        try (var scope = StructuredTaskScope.open(awaitAllSuccessfulOrThrow(),
                taskScopeConfiguration)) {
            do {
                var clientCall = calls.next(context);
                scope.fork(() -> {
                    context.setInnerRun(this.innerRun.incrementAndGet());
                    try {
                        this.gate.acquire();
                        clientCall.call();
                        var response = context.getResponseContext()
                                .orElseThrow();

                        CLILogger.info("Response from server : [%d-%s]",
                                response.statusCode(), response.body());
                    } catch (Exception e) {
                        this.onFailure(e, context);
                    } finally {
                        this.gate.release();
                    }
                });
            } while (calls.hasNext());

            scope.join();
        } catch (Exception e) {
            System.out.println("last failure");
            this.onFailure(e, context);
        }
    }

    //todo: this logs appear duplicated, try to understand why - all from inside scope
    private <E extends Throwable> void onFailure(E failure, RunContext context) {
        Objects.requireNonNull(failure);
        var message = cleanMessage(failure);
        CLILogger.severe("Requests failed: [%s]", message);
        if (failure instanceof InterruptedException interrupted) {
            CLILogger.warn("Thread was interrupted: [%s]", interrupted.getMessage());
            Thread.currentThread().interrupt();

            return;
        }

        CLILogger.severe(templateTerminationErrorMessage.apply(context));
        System.exit(1);
    }

    private <E extends Throwable> String cleanMessage(E failure) {
        return failure.getMessage() != null ? failure.getMessage() : "Error occurred";
    }


    interface RequestIterator extends Iterator<Callable<Void>> {

        Callable<Void> next(RunContext context);

        @Override
        default Callable<Void> next() {
            return this.next(new RunContext(0));
        }
    }

    static class RunContext {
        private static final String RESPONSE_KEY = "response";
        private static final String REQUEST_KEY = "request";
        private static final String REQUEST_BODY_KEY = "request_body";

        private final int run;
        private volatile int innerRun;
        private final ConcurrentMap<ContextKey, ConcurrentMap<String, Object>> context = new ConcurrentHashMap<>();

        RunContext(int run) {
            this.run = run;
        }

        private void setInnerRun(int innerRun) {
            this.innerRun = innerRun;
        }

        //at the moment response has only 1 context
        //keep it as single to not induce errors
        public Optional<HttpResponse<String>> getResponseContext() {
            if (!context.containsKey(ContextKey.RESPONSE) || context.get(ContextKey.RESPONSE).containsKey(RESPONSE_KEY)) {
                return Optional.empty();
            }

            return Optional.ofNullable((HttpResponse<String>) this.context.get(ContextKey.RESPONSE).get(RESPONSE_KEY));
        }

        public Optional<HttpRequest> getHttpRequest() {
            if (!context.containsKey(ContextKey.REQUEST) || context.get(ContextKey.REQUEST).containsKey(REQUEST_KEY)) {
                return Optional.empty();
            }

            return Optional.ofNullable((HttpRequest) this.context.get(ContextKey.REQUEST).get(REQUEST_KEY));
        }

        public Optional<String> getHttpRequestBody() {
            if (!context.containsKey(ContextKey.REQUEST) || context.get(ContextKey.REQUEST).containsKey(REQUEST_BODY_KEY)) {
                return Optional.empty();
            }

            return Optional.ofNullable((String) this.context.get(ContextKey.REQUEST).get(REQUEST_BODY_KEY));
        }

        public void setContext(ContextKey key, Object value) {
            Objects.requireNonNull(key);
            Objects.requireNonNull(value);

            if (context.containsKey(key)) {
                var innerKey = getInnerKey(key, value);

                this.context.get(key).put(innerKey, value);
            } else {
                var values = new ConcurrentHashMap<String, Object>();
                values.put(getInnerKey(key, value), value);
                this.context.put(key, values);
            }
        }

        private String getInnerKey(ContextKey key, Object value) {
            return switch (key) {
                case REQUEST -> value instanceof HttpRequest ? REQUEST_KEY : REQUEST_BODY_KEY;
                case RESPONSE -> RESPONSE_KEY;
            };
        }
    }

    enum ContextKey {
        REQUEST, RESPONSE
    }
}
