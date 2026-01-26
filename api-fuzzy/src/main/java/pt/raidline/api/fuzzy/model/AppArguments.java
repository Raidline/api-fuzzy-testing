package pt.raidline.api.fuzzy.model;

import java.util.Objects;

public class AppArguments {
    public final Arg<String> file;
    public final Arg<String> server;
    public final Arg<Long> maxTime;
    public final Arg<Integer> concurrentCallsGate;

    private AppArguments(Arg<String> file, Arg<String> server, Arg<Long> maxTime,
                         Arg<Integer> concurrentCallsGate) {
        Objects.requireNonNull(file);
        Objects.requireNonNull(server);
        this.maxTime = maxTime;
        this.concurrentCallsGate = concurrentCallsGate;
        this.file = file;
        this.server = server;
    }

    public static AppArgumentsBuilder toBuilder() {
        return new AppArgumentsBuilder();
    }

    public static class AppArgumentsBuilder {
        private Arg<String> file = null;
        private Arg<String> server = null;
        private Arg<Long> maxTime = null;
        private Arg<Integer> concurrentCallsGate = null;

        public AppArgumentsBuilder addFile(String key, String value) {
            this.file = new Arg<>(key, value);

            return this;
        }

        public AppArgumentsBuilder addServer(String key, String value) {
            this.server = new Arg<>(key, value);

            return this;
        }

        public AppArgumentsBuilder addMaxTime(String key, long value) {
            this.maxTime = new Arg<>(key, value);

            return this;
        }

        public AppArgumentsBuilder addConcurrentCallsGate(String key, int value) {
            this.concurrentCallsGate = new Arg<>(key, value);

            return this;
        }

        public AppArguments build() {
            if (this.maxTime == null) {
                this.maxTime = new Arg<>("-t", Long.MAX_VALUE);
            }

            if (this.concurrentCallsGate == null) {
                this.concurrentCallsGate = new Arg<>("-r", 10);
            }

            return new AppArguments(file, server, maxTime, concurrentCallsGate);
        }
    }

    public record Arg<T>(String key, T value) {
    }
}
