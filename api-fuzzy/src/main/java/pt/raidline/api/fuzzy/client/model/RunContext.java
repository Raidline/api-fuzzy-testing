package pt.raidline.api.fuzzy.client.model;

import pt.raidline.api.fuzzy.client.FuzzyClient;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

//todo: this could contain all the requests / responses contexts.
//todo: after X elements we flush to a .txt to make room for new ones
public class RunContext {
    private static final String RESPONSE_KEY = "response";
    private static final String REQUEST_KEY = "request";
    private static final String REQUEST_BODY_KEY = "request_body";

    public final int run;
    public volatile int innerRun;
    private final ConcurrentMap<ContextKey, ConcurrentMap<String, Object>> context = new ConcurrentHashMap<>();

    public RunContext(int run) {
        this.run = run;
    }

    //at the moment response has only 1 context
    //keep it as single to not induce errors
    public Optional<HttpResponse<String>> getResponseContext() {
        if (!context.containsKey(ContextKey.RESPONSE) || !context.get(ContextKey.RESPONSE).containsKey(RESPONSE_KEY)) {
            return Optional.empty();
        }

        return Optional.ofNullable((HttpResponse<String>) this.context.get(ContextKey.RESPONSE).get(RESPONSE_KEY));
    }

    public Optional<HttpRequest> getHttpRequest() {
        if (!context.containsKey(ContextKey.REQUEST) || !context.get(ContextKey.REQUEST).containsKey(REQUEST_KEY)) {
            return Optional.empty();
        }

        return Optional.ofNullable((HttpRequest) this.context.get(ContextKey.REQUEST).get(REQUEST_KEY));
    }

    public Optional<String> getHttpRequestBody() {
        if (!context.containsKey(ContextKey.REQUEST) || !context.get(ContextKey.REQUEST).containsKey(REQUEST_BODY_KEY)) {
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

    public enum ContextKey {
        REQUEST, RESPONSE
    }
}
