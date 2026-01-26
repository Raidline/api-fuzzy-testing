package pt.raidline.api.fuzzy;

import pt.raidline.api.fuzzy.logging.CLILogger;
import pt.raidline.api.fuzzy.model.AppArguments;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class TestController {
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Duration maxTime;
    private long startTime;
    public volatile boolean timeoutReached = false;

    public TestController(AppArguments.Arg<Long> maxTime) {
        this.maxTime = Duration.of(maxTime.value(), ChronoUnit.SECONDS);
    }

    public void init() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            CLILogger.warn("Shutdown signal received! Cleaning up...");
            running.set(false);
        }));
    }

    public void startTimeoutCheck() {
        this.startTime = System.nanoTime();

        CLILogger.info("Initiating thread to check timeout");

        Thread.ofVirtual().name("Timeout-Checker").start(() -> {
            while (!timeoutReached) {
                timeoutReached = hasTimeoutBeenReached();
            }

            CLILogger.info("Defined timeout of [%s], has been reached. Ending Program as soon as possible",
                    maxTime.toString());
        });
    }

    public boolean shouldStop() {
        return !running.get() || Thread.currentThread().isInterrupted();
    }

    private boolean hasTimeoutBeenReached() {
        Duration elapsed = Duration.ofNanos(System.nanoTime() - startTime);
        boolean reached = elapsed.compareTo(maxTime) > 0;
        timeoutReached = reached;

        return reached;
    }
}
