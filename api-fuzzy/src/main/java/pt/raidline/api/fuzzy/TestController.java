package pt.raidline.api.fuzzy;

import pt.raidline.api.fuzzy.logging.CLILogger;

import java.util.concurrent.atomic.AtomicBoolean;

public class TestController {
    private final AtomicBoolean running = new AtomicBoolean(true);

    public void init() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            CLILogger.warn("Shutdown signal received! Cleaning up...");
            running.set(false);
        }));
    }

    public boolean shouldContinue() {
        return running.get() && !Thread.currentThread().isInterrupted();
    }
}
