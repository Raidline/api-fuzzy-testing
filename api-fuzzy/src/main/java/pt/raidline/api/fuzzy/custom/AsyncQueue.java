package pt.raidline.api.fuzzy.custom;

import pt.raidline.api.fuzzy.logging.CLILogger;

import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;

public class AsyncQueue {
    private static final int MAX_REQUESTS = 10;
    private final int max;
    private final AtomicInteger count;
    private final LinkedList<CompletableFuture<Void>> requests;
    private final ReentrantLock lock = new ReentrantLock();

    public AsyncQueue() {
        this(MAX_REQUESTS);
    }

    public AsyncQueue(int max) {
        this.max = max;
        this.requests = new LinkedList<>();
        this.count = new AtomicInteger(0);
        //LinkedBlockingDeque -> probably
    }

    public boolean enqueue(Supplier<CompletableFuture<Void>> request) {
        if (count.get() >= max) {
            CLILogger.warn("Could not submit more requests to the queue");
            return false;
        }


        lock.lock();
        var req = request.get();
        try {
            this.requests.offerLast(req);
            this.count.incrementAndGet();
        } finally {
            lock.unlock();

            //will we have deadlock?
            req.whenComplete((__, ignored) -> this.dequeue());
        }

        return true;
    }

    private void dequeue() {
        if (count.get() == 0) {
            return;
        }

        lock.lock();
        try {
            //does not need to be the one that originated the requests, we just need to space
            this.requests.pop();
        } finally {
            lock.unlock();
        }
    }

    public void syncAll() {
        if (requests.isEmpty()) {
            CLILogger.info("Completed all requests for all cycles");

            return;
        }

        lock.lock();
        try {
            var onHoldFutures = requests.toArray(CompletableFuture[]::new);

            CompletableFuture.allOf(onHoldFutures).join();
        } finally {
            lock.unlock();
        }
    }
}
