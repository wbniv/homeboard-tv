package com.gathin.tvhome;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Use everywhere: every change Homeboard makes to this TV's Android screensaver, through ScreensaverGateway.
 * Each change reads the current values first, keeps the original before the first write, writes, reads every
 * written value back, and on any failure puts back what it found and reports a typed Result; it never throws.
 * The managed flag changes only after Android has accepted the values. Unit tested with a fake gateway.
 */
final class EverywhereScreensaver {
    enum Result { OK, UNREADABLE, NOT_PERMITTED, REFUSED, READBACK_DIFFERS }

    static final class Outcome {
        final Result result;
        /** What Android held before the attempt; null when it could not be read. Undo entries carry it. */
        final ScreensaverValues before;
        /** The component list as read at the end, after any revert: what the failure notice says is still in use. */
        final ScreensaverGateway.Reading now;
        Outcome(Result result, ScreensaverValues before, ScreensaverGateway.Reading now) {
            this.result = result; this.before = before; this.now = now;
        }
        boolean ok() { return result == Result.OK; }
    }

    /** What the Screensaver screens show: read fresh each time, changing nothing. */
    static final class Status {
        final boolean managed, mayWrite;
        final ScreensaverGateway.Reading component, timeout;
        final ScreensaverValues original;
        Status(boolean managed, boolean mayWrite, ScreensaverGateway.Reading component, ScreensaverGateway.Reading timeout, ScreensaverValues original) {
            this.managed = managed; this.mayWrite = mayWrite; this.component = component; this.timeout = timeout; this.original = original;
        }
    }

    private final ScreensaverGateway gateway;
    private final ScreensaverDeviceStore store;

    EverywhereScreensaver(ScreensaverGateway gateway, ScreensaverDeviceStore store) { this.gateway = gateway; this.store = store; }

    ScreensaverGateway gateway() { return gateway; }
    boolean managed() { return store.managed(); }
    ScreensaverValues original() { return store.original(); }

    Status status() {
        return new Status(store.managed(), gateway.mayWrite(), gateway.readSecure(ScreensaverGateway.COMPONENTS),
                gateway.readTimeout(), store.original());
    }

    /** The asked-once Confirm: keep the original, make component this TV's screensaver, and mark the TV managed. */
    Outcome manage(String component) { return change(component, true); }

    /** A later choice while managed: applied straight away, no question. */
    Outcome apply(String component) { return change(component, false); }

    private Outcome change(String component, boolean first) {
        ScreensaverValues before = ScreensaverValues.read(gateway);
        if (before == null) return outcome(Result.UNREADABLE, null);
        ScreensaverValues target = before.target(component);
        boolean writes = !target.equals(before);
        if (writes && !gateway.mayWrite()) return outcome(Result.NOT_PERMITTED, before);
        // Kept before the first write, so a failure part way through still leaves the way back.
        if (!store.recordOriginal(before)) return outcome(Result.REFUSED, before);
        if (writes) {
            Result written = write(before, target);
            if (written != Result.OK) return outcome(written, before);
        }
        if (first && !store.setManaged(true)) {
            if (writes) putBack(before);
            return outcome(Result.REFUSED, before);
        }
        return outcome(Result.OK, before);
    }

    /** Restore the TV's original: puts the kept values back, verifies them and ends Homeboard's management. */
    Outcome restore() {
        ScreensaverValues original = store.original();
        ScreensaverValues before = ScreensaverValues.read(gateway);
        if (before == null || original == null) return outcome(Result.UNREADABLE, before);
        return putAndSetManaged(before, original, false);
    }

    /**
     * Undo of a change: puts back the previous values the Undo entry carries, which need not be the original, and
     * the managed flag as it was. A previous component whose app is gone is refused, unless it is the original or
     * Homeboard's own; installed holds the flattened components of the installed screensaver apps.
     */
    Outcome undoTo(ScreensaverValues previous, boolean managedBefore, Collection<String> installed) {
        ScreensaverValues before = ScreensaverValues.read(gateway);
        if (before == null || previous == null) return outcome(Result.UNREADABLE, before);
        if (!available(previous.component(), installed)) return outcome(Result.REFUSED, before);
        return putAndSetManaged(before, previous, managedBefore);
    }

    /**
     * The chosen Android screensaver app was uninstalled. Returns null when choice is not such an app or is still
     * installed. Otherwise, when Homeboard manages the TV, restores the original; when it does not, changes nothing.
     * The caller falls back to the default slot and says so once.
     */
    Outcome fallBackIfGone(String choice, Collection<String> installed) {
        if (!ScreensaverRules.isApp(choice) || installed.contains(choice)) return null;
        if (!store.managed()) return outcome(Result.OK, ScreensaverValues.read(gateway));
        return restore();
    }

    private boolean available(String component, Collection<String> installed) {
        String first = ScreensaverRules.first(component);
        if (first == null || first.equals(gateway.ownComponent()) || installed.contains(first)) return true;
        ScreensaverValues original = store.original();
        return original != null && Objects.equals(component, original.component());
    }

    private Outcome putAndSetManaged(ScreensaverValues before, ScreensaverValues values, boolean managed) {
        boolean writes = differs(before, values);
        if (writes && !gateway.mayWrite()) return outcome(Result.NOT_PERMITTED, before);
        if (writes) {
            Result written = write(before, values);
            if (written != Result.OK) return outcome(written, before);
        }
        if (!store.setManaged(managed)) {
            if (writes) putBack(before);
            return outcome(Result.REFUSED, before);
        }
        return outcome(Result.OK, before);
    }

    private static boolean differs(ScreensaverValues from, ScreensaverValues to) {
        for (String key : to.keys()) if (!from.has(key) || !Objects.equals(from.get(key), to.get(key))) return true;
        return false;
    }

    /** Writes each value of to that differs from from, reading each back; on any failure puts from back. */
    private Result write(ScreensaverValues from, ScreensaverValues to) {
        List<String> touched = new ArrayList<>();
        for (String key : to.keys()) {
            if (from.has(key) && Objects.equals(from.get(key), to.get(key))) continue;
            touched.add(key);
            ScreensaverGateway.Write write = gateway.writeSecure(key, to.get(key));
            if (write != ScreensaverGateway.Write.OK) {
                putBack(from, touched);
                return write == ScreensaverGateway.Write.NOT_PERMITTED ? Result.NOT_PERMITTED : Result.REFUSED;
            }
            ScreensaverGateway.Reading back = gateway.readSecure(key);
            if (!back.readable || !Objects.equals(back.value, to.get(key))) {
                putBack(from, touched);
                return Result.READBACK_DIFFERS;
            }
        }
        return Result.OK;
    }

    private void putBack(ScreensaverValues values) {
        List<String> keys = new ArrayList<>();
        for (String key : values.keys()) keys.add(key);
        putBack(values, keys);
    }

    /** Best effort: each key that now reads differently from values gets its old value written again. */
    private void putBack(ScreensaverValues values, List<String> keys) {
        for (String key : keys) {
            if (!values.has(key)) continue;
            ScreensaverGateway.Reading now = gateway.readSecure(key);
            if (now.readable && Objects.equals(now.value, values.get(key))) continue;
            gateway.writeSecure(key, values.get(key));
        }
    }

    private Outcome outcome(Result result, ScreensaverValues before) {
        return new Outcome(result, before, gateway.readSecure(ScreensaverGateway.COMPONENTS));
    }
}
