package pt.raidline.api.fuzzy.model;

import java.util.Objects;

public class AppArguments {
    public final Arg file;
    public final Arg server;

    private AppArguments(Arg file, Arg server) {
        Objects.requireNonNull(file);
        Objects.requireNonNull(server);
        this.file = file;
        this.server = server;
    }

    public static AppArgumentsBuilder toBuilder() {
        return new AppArgumentsBuilder();
    }

    public static class AppArgumentsBuilder {
        private Arg file = null;
        private Arg server = null;

        public AppArgumentsBuilder addFile(String key, String value) {
            this.file = new Arg(key, value);

            return this;
        }

        public AppArgumentsBuilder addServer(String key, String value) {
            this.server = new Arg(key, value);

            return this;
        }

        public AppArguments build() {
            return new AppArguments(file, server);
        }
    }

    public record Arg(String key, String value) {
    }
}
