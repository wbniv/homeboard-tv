package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AccessibilityListTest {
    // A service whose class is inside its package, so Android may write it either way.
    private static final String MINE = "com.example.launcher/com.example.launcher.HomeKeyService";
    private static final String MINE_SHORT = "com.example.launcher/.HomeKeyService";
    // Homeboard's own: the package (net.biohack.homeboard) is not the Java namespace, so there is no short form.
    private static final String HOMEBOARD = "net.biohack.homeboard/com.gathin.tvhome.HomeKeyService";
    private static final String TALKBACK = "com.google.android.marvin.talkback/com.google.android.marvin.talkback.TalkBackService";
    private static final String OTHER = "com.example.keys/.KeyService";

    @Test public void addsToAnEmptyOrMissingList() {
        assertEquals(MINE, AccessibilityList.with(null, MINE));
        assertEquals(MINE, AccessibilityList.with("", MINE));
        assertEquals(MINE, AccessibilityList.with("null", MINE));
    }

    @Test public void appendsAfterOthersAndKeepsTheirSpelling() {
        assertEquals(TALKBACK + ":" + OTHER + ":" + MINE, AccessibilityList.with(TALKBACK + ":" + OTHER, MINE));
    }

    @Test public void addingWhatIsThereChangesNothingInEitherSpelling() {
        assertEquals(OTHER + ":" + MINE_SHORT, AccessibilityList.with(OTHER + ":" + MINE_SHORT, MINE));
        assertEquals(MINE + ":" + OTHER, AccessibilityList.with(MINE + ":" + OTHER, MINE_SHORT));
    }

    @Test public void duplicatesOfOursCollapseToTheFirst() {
        assertEquals(MINE + ":" + OTHER, AccessibilityList.with(MINE + ":" + OTHER + ":" + MINE_SHORT, MINE));
    }

    @Test public void removesOnlyOursInEverySpelling() {
        assertEquals(TALKBACK + ":" + OTHER, AccessibilityList.without(TALKBACK + ":" + MINE + ":" + OTHER + ":" + MINE_SHORT, MINE));
    }

    @Test public void removingTheLastEntryLeavesEmpty() {
        assertEquals("", AccessibilityList.without(MINE, MINE_SHORT));
        assertEquals("", AccessibilityList.without(null, MINE));
        assertEquals(OTHER, AccessibilityList.without(OTHER, MINE));
    }

    @Test public void emptyEntriesAndStrayColonsAreDropped() {
        assertEquals(OTHER + ":" + MINE, AccessibilityList.with("::" + OTHER + "::", MINE));
        assertEquals(OTHER, AccessibilityList.without(":" + MINE + "::" + OTHER + ":", MINE));
    }

    @Test public void containsMatchesBothSpellingsOnly() {
        assertTrue(AccessibilityList.contains(OTHER + ":" + MINE_SHORT, MINE));
        assertTrue(AccessibilityList.contains(MINE, MINE_SHORT));
        assertFalse(AccessibilityList.contains(OTHER, MINE));
        assertFalse(AccessibilityList.contains("com.example.launcher.debug/com.example.launcher.HomeKeyService", MINE));
        assertFalse(AccessibilityList.contains(null, MINE));
    }

    @Test public void homeboardsReleaseAndDebugHelpersAreDifferentEntries() {
        String debug = "net.biohack.homeboard.debug/com.gathin.tvhome.HomeKeyService";
        assertTrue(AccessibilityList.contains(debug + ":" + HOMEBOARD, HOMEBOARD));
        assertEquals(debug, AccessibilityList.without(debug + ":" + HOMEBOARD, HOMEBOARD));
        assertEquals(debug + ":" + HOMEBOARD, AccessibilityList.with(debug, HOMEBOARD));
        assertFalse(AccessibilityList.contains(debug, HOMEBOARD));
    }

    @Test public void aMalformedEntryIsKeptAndNeverMatches() {
        assertEquals("garbage:" + HOMEBOARD, AccessibilityList.with("garbage", HOMEBOARD));
        assertEquals("garbage", AccessibilityList.without("garbage:" + HOMEBOARD, HOMEBOARD));
    }
}
