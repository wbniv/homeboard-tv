package com.gathin.tvhome;

/** Automatic fixes are runtime/cache state, independent of the user's layout and Undo. */
final class RainLocation {
    static final long MAX_AGE = 6 * 60 * 60_000L;
    static final long REFRESH = 15 * 60_000L;
    static final long RETRY = 60_000L;
    double latitude = Double.NaN, longitude = Double.NaN;
    long fixedAt, attemptedAt = -1;
    boolean pending, failed;
    int generation;

    static boolean recent(long timestamp, long now, long limit) {
        return timestamp > 0 && now >= timestamp && now - timestamp < limit;
    }
    boolean located() { return RainSettings.coordinates(latitude, longitude); }
    boolean usable(long now) { return located() && recent(fixedAt, now, MAX_AGE); }
    boolean accept(double lat, double lon, long timestamp, long now) {
        if (!RainSettings.coordinates(lat, lon) || !recent(timestamp, now, MAX_AGE)) return false;
        latitude = Math.round(lat * 1000) / 1000.0;
        longitude = Math.round(lon * 1000) / 1000.0;
        fixedAt = timestamp;
        return true;
    }
    RainSettings effective(RainSettings configured, boolean granted) {
        RainSettings result = configured.copy();
        if (configured.automatic) {
            result.latitude = granted ? latitude : Double.NaN;
            result.longitude = granted ? longitude : Double.NaN;
        }
        return result;
    }
    int begin(long elapsed, boolean manual) {
        if (pending || !manual && attemptedAt >= 0 && elapsed - attemptedAt < (failed ? RETRY : REFRESH)) return -1;
        pending = true; attemptedAt = elapsed;
        return ++generation;
    }
    boolean finish(int token, boolean success) {
        if (!pending || token != generation) return false;
        pending = false; failed = !success;
        return true;
    }
    void cancel() { generation++; pending = false; attemptedAt = -1; }
}
