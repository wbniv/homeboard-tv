package com.gathin.tvhome;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.gathin.tvhome.FakeScreensaverGateway.AERIAL;
import static com.gathin.tvhome.FakeScreensaverGateway.AMBIENT;
import static com.gathin.tvhome.FakeScreensaverGateway.OWN;
import static com.gathin.tvhome.ScreensaverGateway.ACTIVATE_ON_SLEEP;
import static com.gathin.tvhome.ScreensaverGateway.COMPONENTS;
import static com.gathin.tvhome.ScreensaverGateway.ENABLED;
import static org.junit.Assert.*;

/** Use everywhere against an in-memory Android: every change is verified, every failure changes nothing. */
public class EverywhereScreensaverTest {
    private FakeScreensaverGateway android;
    private FakeScreensaverGateway.MemoryBacking backing;
    private ScreensaverDeviceStore store;
    private EverywhereScreensaver everywhere;
    private final Set<String> installed = new HashSet<>(Collections.singletonList(AERIAL));

    @Before public void setUp() {
        android = new FakeScreensaverGateway();
        backing = new FakeScreensaverGateway.MemoryBacking();
        store = new ScreensaverDeviceStore(backing);
        everywhere = new EverywhereScreensaver(android, store);
    }

    private Map<String, String> secureBefore() { return new HashMap<>(android.secure); }

    @Test public void confirmKeepsTheOriginalWritesReadsBackAndMarksTheTvManaged() {
        EverywhereScreensaver.Outcome outcome = everywhere.manage(OWN);
        assertEquals(EverywhereScreensaver.Result.OK, outcome.result);
        assertEquals(OWN, android.secure.get(COMPONENTS));
        assertEquals(Collections.singletonList(COMPONENTS + "=" + OWN), android.writes);   // enabled and sleep were already on
        assertTrue(store.managed());
        assertEquals(AMBIENT, store.original().component());
        assertEquals(AMBIENT, outcome.before.component());   // the Undo entry's previous value
        assertEquals(OWN, outcome.now.value);
    }

    @Test public void theOriginalIsRecordedOnceAndNeverOverwritten() {
        everywhere.manage(OWN);
        everywhere.apply(AERIAL);
        assertEquals(AMBIENT, store.original().component());
        everywhere.restore();
        android.secure.put(COMPONENTS, "com.other/com.other.Saver");   // changed outside Homeboard later
        assertTrue(everywhere.manage(OWN).ok());
        assertEquals(AMBIENT, store.original().component());
        assertTrue(store.recordOriginal(ScreensaverValues.read(android)));   // a second record is a no-op
        assertEquals(AMBIENT, store.original().component());
    }

    @Test public void withoutThePermissionNothingIsWrittenAndNothingChanges() {
        android.permitted = false;
        Map<String, String> before = secureBefore();
        EverywhereScreensaver.Outcome outcome = everywhere.manage(AERIAL);
        assertEquals(EverywhereScreensaver.Result.NOT_PERMITTED, outcome.result);
        assertTrue(android.writes.isEmpty());
        assertEquals(before, android.secure);
        assertFalse(store.managed());
        assertNull(store.original());
        assertEquals(AMBIENT, outcome.now.value);   // "This TV's screensaver is still Ambient"
    }

    @Test public void aRefusedWriteLeavesTheSettingAndTheStateUnchanged() {
        android.refused.add(COMPONENTS);
        Map<String, String> before = secureBefore();
        EverywhereScreensaver.Outcome outcome = everywhere.manage(AERIAL);
        assertEquals(EverywhereScreensaver.Result.REFUSED, outcome.result);
        assertEquals(before, android.secure);
        assertFalse(store.managed());
        assertEquals(AMBIENT, outcome.now.value);
        // The original found before the attempt may be kept; it equals what is still there.
        assertEquals(AMBIENT, store.original().component());
    }

    @Test public void aReadBackMismatchIsReportedAndPutBack() {
        android.storesInstead.put(COMPONENTS, "com.vendor/com.vendor.Default");
        EverywhereScreensaver.Outcome outcome = everywhere.manage(AERIAL);
        assertEquals(EverywhereScreensaver.Result.READBACK_DIFFERS, outcome.result);
        assertEquals(AMBIENT, android.secure.get(COMPONENTS));
        assertEquals(Arrays.asList(COMPONENTS + "=" + AERIAL, COMPONENTS + "=" + AMBIENT), android.writes);
        assertFalse(store.managed());
        assertEquals(AMBIENT, outcome.now.value);
    }

    @Test public void whenEvenThePutBackFailsTheNoticeNamesWhatAndroidNowHolds() {
        android.alwaysStoresInstead.put(COMPONENTS, "com.vendor/com.vendor.Default");
        EverywhereScreensaver.Outcome outcome = everywhere.manage(AERIAL);
        assertEquals(EverywhereScreensaver.Result.READBACK_DIFFERS, outcome.result);
        assertEquals("com.vendor/com.vendor.Default", outcome.now.value);
        assertFalse(store.managed());
    }

    @Test public void aFailureHalfwayPutsTheEarlierWritesBack() {
        android.secure.put(ENABLED, "0");
        android.refused.add(ENABLED);
        EverywhereScreensaver.Outcome outcome = everywhere.manage(AERIAL);
        assertEquals(EverywhereScreensaver.Result.REFUSED, outcome.result);
        assertEquals(AMBIENT, android.secure.get(COMPONENTS));
        assertEquals("0", android.secure.get(ENABLED));
        assertFalse(store.managed());
    }

    @Test public void enabledAndSleepAreSwitchedOnOnlyWhenOffAndComeBackWithRestore() {
        android.secure.put(ENABLED, "0");
        android.secure.remove(ACTIVATE_ON_SLEEP);   // not set: follows the TV's default, left alone
        assertTrue(everywhere.manage(OWN).ok());
        assertEquals("1", android.secure.get(ENABLED));
        assertFalse(android.secure.containsKey(ACTIVATE_ON_SLEEP));
        assertEquals("0", store.original().get(ENABLED));
        assertTrue(everywhere.restore().ok());
        assertEquals(AMBIENT, android.secure.get(COMPONENTS));
        assertEquals("0", android.secure.get(ENABLED));
        assertFalse(android.secure.containsKey(ACTIVATE_ON_SLEEP));
        assertFalse(store.managed());
    }

    @Test public void restorePutsTheOriginalBackVerifiedAndEndsManagement() {
        everywhere.manage(OWN);
        everywhere.apply(AERIAL);
        EverywhereScreensaver.Outcome outcome = everywhere.restore();
        assertTrue(outcome.ok());
        assertEquals(AERIAL, outcome.before.component());   // Undo of Restore puts Aerial back
        assertEquals(AMBIENT, android.secure.get(COMPONENTS));
        assertFalse(store.managed());
    }

    @Test public void restoreWithNothingKeptOrNothingReadableWritesNothing() {
        assertEquals(EverywhereScreensaver.Result.UNREADABLE, everywhere.restore().result);
        everywhere.manage(OWN);
        android.writes.clear();
        android.unreadable.add(COMPONENTS);
        assertEquals(EverywhereScreensaver.Result.UNREADABLE, everywhere.restore().result);
        assertTrue(android.writes.isEmpty());
        assertTrue(store.managed());
    }

    @Test public void undoPutsBackThePreviousValueNotTheOriginal() {
        EverywhereScreensaver.Outcome first = everywhere.manage(OWN);
        EverywhereScreensaver.Outcome second = everywhere.apply(AERIAL);
        assertTrue(everywhere.undoTo(second.before, true, installed).ok());
        assertEquals(OWN, android.secure.get(COMPONENTS));
        assertTrue(store.managed());
        assertTrue(everywhere.undoTo(first.before, false, installed).ok());
        assertEquals(AMBIENT, android.secure.get(COMPONENTS));
        assertFalse(store.managed());
    }

    @Test public void undoToAnUninstalledAppIsRefusedButTheOriginalIsAlwaysAllowed() {
        everywhere.manage(OWN);
        EverywhereScreensaver.Outcome toAerial = everywhere.apply(AERIAL);
        EverywhereScreensaver.Outcome toOwn = everywhere.apply(OWN);
        android.writes.clear();
        assertEquals(EverywhereScreensaver.Result.REFUSED, everywhere.undoTo(toOwn.before, true, Collections.emptySet()).result);
        assertTrue(android.writes.isEmpty());
        assertTrue(everywhere.undoTo(toAerial.before, true, Collections.emptySet()).ok());   // Homeboard's own
        assertTrue(everywhere.undoTo(store.original(), false, Collections.emptySet()).ok());   // Ambient is not "installed" here
        assertEquals(AMBIENT, android.secure.get(COMPONENTS));
    }

    @Test public void undoWithoutThePermissionChangesNothing() {
        EverywhereScreensaver.Outcome change = everywhere.manage(AERIAL);
        android.permitted = false;
        assertEquals(EverywhereScreensaver.Result.NOT_PERMITTED, everywhere.undoTo(change.before, false, installed).result);
        assertEquals(AERIAL, android.secure.get(COMPONENTS));
        assertTrue(store.managed());
    }

    @Test public void anUninstalledManagedAppFallsBackAndRestoresTheOriginal() {
        assertTrue(everywhere.manage(AERIAL).ok());
        assertNull(everywhere.fallBackIfGone(AERIAL, installed));   // still installed
        assertNull(everywhere.fallBackIfGone(ScreensaverRegistry.CLOCK, Collections.emptySet()));   // not an app
        EverywhereScreensaver.Outcome outcome = everywhere.fallBackIfGone(AERIAL, Collections.emptySet());
        assertNotNull(outcome);
        assertTrue(outcome.ok());
        assertEquals(AMBIENT, android.secure.get(COMPONENTS));
        assertFalse(store.managed());
    }

    @Test public void anUninstalledAppOnHomeOnlyChangesNothingInAndroid() {
        EverywhereScreensaver.Outcome outcome = everywhere.fallBackIfGone(AERIAL, Collections.emptySet());
        assertTrue(outcome.ok());
        assertTrue(android.writes.isEmpty());
    }

    @Test public void anUnreadableSettingIsUnknownAndNothingIsWritten() {
        android.unreadable.add(COMPONENTS);
        android.timeoutReadable = false;
        EverywhereScreensaver.Outcome outcome = everywhere.manage(OWN);
        assertEquals(EverywhereScreensaver.Result.UNREADABLE, outcome.result);
        assertNull(outcome.before);
        assertTrue(android.writes.isEmpty());
        assertNull(store.original());
        EverywhereScreensaver.Status status = everywhere.status();
        assertFalse(status.component.readable);
        assertEquals(ScreensaverRules.OtherLine.UNKNOWN, ScreensaverRules.otherLine(status.managed, status.component.readable));
        assertEquals(ScreensaverRules.UNKNOWN, ScreensaverRules.timeoutMinutes(status.timeout));
    }

    @Test public void anUnreadableEnabledFlagIsLeftAloneAndNotKept() {
        android.unreadable.add(ENABLED);
        assertTrue(everywhere.manage(OWN).ok());
        assertFalse(store.original().has(ENABLED));
        for (String write : android.writes) assertFalse(write, write.startsWith(ENABLED));
    }

    @Test public void ifTheOriginalCannotBeStoredNothingIsWritten() {
        backing.allowedPuts = 0;
        assertEquals(EverywhereScreensaver.Result.REFUSED, everywhere.manage(OWN).result);
        assertTrue(android.writes.isEmpty());
        assertFalse(store.managed());
    }

    @Test public void ifTheManagedFlagCannotBeStoredAndroidIsPutBack() {
        backing.allowedPuts = 1;   // the original is kept, the managed flag is not
        assertEquals(EverywhereScreensaver.Result.REFUSED, everywhere.manage(OWN).result);
        assertEquals(AMBIENT, android.secure.get(COMPONENTS));
        assertFalse(store.managed());
    }

    @Test public void switchingBetweenHomeboardEntriesNeedsNoWriteAndNoPermission() {
        everywhere.manage(OWN);
        android.permitted = false;
        android.writes.clear();
        EverywhereScreensaver.Outcome outcome = everywhere.apply(OWN);
        assertTrue(outcome.ok());
        assertTrue(android.writes.isEmpty());
    }

    @Test public void startNowGoesThroughTheGateway() {
        assertTrue(everywhere.gateway().startNow());
        android.startNowWorks = false;
        assertFalse(everywhere.gateway().startNow());
        assertEquals(2, android.startNowCalls);
    }
}
