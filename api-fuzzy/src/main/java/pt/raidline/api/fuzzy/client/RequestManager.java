package pt.raidline.api.fuzzy.client;

import pt.raidline.api.fuzzy.assertions.AssertionUtils;
import pt.raidline.api.fuzzy.logging.CLILogger;

import java.net.http.HttpResponse;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;

import static java.util.concurrent.StructuredTaskScope.Joiner.awaitAllSuccessfulOrThrow;
import static pt.raidline.api.fuzzy.assertions.AssertionUtils.internalAssertion;

public class RequestManager {
    private static final ThreadFactory threadFactory = Thread.ofVirtual()
            .name("Fuzzy-Tester-VT", 0)
            .factory();
    private static final Function<StructuredTaskScope.Configuration, StructuredTaskScope.Configuration> taskScopeConfiguration =
            config -> config.withThreadFactory(threadFactory);

    private final Semaphore gate;

    RequestManager(int concurrencyGate) {
        this.gate = new Semaphore(concurrencyGate);
    }

    void submit(RequestIterator<String> calls) {
        Objects.requireNonNull(calls);
        internalAssertion("Submit Requests", calls::hasNext,
                "You cannot submit an empty iterator for tasks");
        try (var scope = StructuredTaskScope.open(awaitAllSuccessfulOrThrow(),
                taskScopeConfiguration)) {
            do {
                var clientCall = calls.next();
                scope.fork(() -> {
                    try {
                        this.gate.acquire();
                        var res = clientCall.call();
                        CLILogger.info("Response from server : [%d-%s]", res.statusCode(), res.body());
                    } catch (Exception e) {
                        this.onFailure(e);
                    } finally {
                        this.gate.release();
                    }
                });
            } while (calls.hasNext());

            scope.join();
        } catch (Exception e) {
            this.onFailure(e);
        }
    }


    private <E extends Throwable> void onFailure(E failure) {
        Objects.requireNonNull(failure);
        var message = cleanMessage(failure);
        CLILogger.severe("Requests failed: [%s]", message);
        if (failure instanceof InterruptedException interrupted) {
            CLILogger.warn("Thread was interrupted: [%s]", interrupted.getMessage());
            Thread.currentThread().interrupt();
        }

        //todo: we need to shutdown here and report back the failure
    }

    private <E extends Throwable> String cleanMessage(E failure) {
        return failure.getMessage() != null ? failure.getMessage() : "Error occurred";
    }


    interface RequestIterator<T> extends Iterator<Callable<HttpResponse<T>>> {
    }
}
