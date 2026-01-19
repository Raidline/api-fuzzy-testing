package pt.raidline.api.fuzzy.custom;

import pt.raidline.api.fuzzy.logging.CLILogger;

import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class AsyncQueue {
    private static final int MAX_REQUESTS = 10;
    private final int max;
    private int count;
    private final LinkedList<CompletableFuture<Void>> requests;
    private final ReentrantLock lock = new ReentrantLock();

    public AsyncQueue() {
        this(MAX_REQUESTS);
    }

    public AsyncQueue(int max) {
        this.max = max;
        this.requests = new LinkedList<>();
        this.count = 0;
    }

    public boolean enqueue(Supplier<CompletableFuture<Void>> request) {
        CompletableFuture<Void> req;

        lock.lock();
        try {
            if (count >= max) {
                return false;
            }
            req = request.get();
            this.requests.offerLast(req);
            this.count++;
        } finally {
            lock.unlock();
        }

        req.whenComplete((__, ignored) -> this.dequeue());
        return true;
    }

    private void dequeue() {
        lock.lock();
        try {
            if (!this.requests.isEmpty()) {
                this.requests.pop();
                this.count--;
            }
        } finally {
            lock.unlock();
        }
    }

    public void syncAll() {
        CompletableFuture<?>[] onHoldFutures;

        lock.lock();
        try {
            if (requests.isEmpty()) {
                CLILogger.info("Completed all requests for all cycles");
                return;
            }
            onHoldFutures = requests.toArray(CompletableFuture[]::new);
            this.count = 0;
            this.requests.clear();
        } finally {
            lock.unlock();
        }

        CompletableFuture.allOf(onHoldFutures).join();
        CLILogger.info("Completed all requests for all cycles");
    }
}
