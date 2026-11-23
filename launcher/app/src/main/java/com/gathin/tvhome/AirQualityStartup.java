package com.gathin.tvhome;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/** One card's background initialization; all state and callbacks belong to the UI executor. */
final class AirQualityStartup<T> {
    private final Executor worker, ui;
    private int generation;
    private boolean pending, ready, failed, closed;
    AirQualityStartup(Executor worker, Executor ui) { this.worker = worker; this.ui = ui; }
    boolean ready() { return ready; }
    boolean failed() { return failed; }
    void start(Callable<T> read, Consumer<T> publish, Runnable failure) {
        if (closed || pending || ready) return;
        pending = true; failed = false;
        int token = ++generation;
        worker.execute(() -> {
            try {
                T loaded = read.call();
                ui.execute(() -> {
                    if (closed || token != generation) return;
                    pending = false; ready = true;
                    publish.accept(loaded);
                });
            } catch (Exception error) {
                ui.execute(() -> {
                    if (closed || token != generation) return;
                    pending = false; failed = true; failure.run();
                });
            }
        });
    }
    void close() { closed = true; generation++; pending = false; }
}
