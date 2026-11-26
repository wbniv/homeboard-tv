package com.gathin.tvhome;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static com.gathin.tvhome.FakeScreensaverGateway.AERIAL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Home's Start screensaver button and the remote's "screensaver" action word start the screensaver through one rule,
 * RemoteKeys.startScreensaver (docs/plans/2026-11-26-top-row-start-screensaver.md). The two callers differ only in
 * how an app's preview screen is started: Home plays it directly, the action word sends Home an intent.
 */
public class StartScreensaverTest {
    private static final String CLOCK = ScreensaverRegistry.CLOCK, BACKGROUND = ScreensaverRegistry.HOME_BACKGROUND;
    private static final String WF_CLOCKS = "org.worldfoundry.clocks/org.worldfoundry.screensaver.ClocksDream";

    /** Records what a caller started; appScreen is named after the caller's own way of starting it. */
    private static final class Recorder implements RemoteKeys.ScreensaverStart {
        final String appScreenWay;
        final List<String> started = new ArrayList<>();
        Recorder(String appScreenWay) { this.appScreenWay = appScreenWay; }
        @Override public void showNow() { started.add("show-now"); }
        @Override public void appScreen() { started.add(appScreenWay); }
        @Override public void androidStartNow() { started.add("android"); }
    }

    private static void sameRuleForBoth(String choice, boolean appScreen, boolean shortcut,
                                        ScreensaverRules.Player expected, String button, String action) {
        Recorder fromButton = new Recorder("play-on-home"), fromAction = new Recorder("intent-to-home");
        assertEquals(expected, RemoteKeys.startScreensaver(choice, appScreen, () -> shortcut, fromButton));
        assertEquals(expected, RemoteKeys.startScreensaver(choice, appScreen, () -> shortcut, fromAction));
        assertEquals(List.of(button), fromButton.started);
        assertEquals(List.of(action), fromAction.started);
    }

    @Test public void aHomeboardEntryPlaysAtOnceThroughShowNowFromBoth() {
        sameRuleForBoth(CLOCK, false, true, ScreensaverRules.Player.HOMEBOARD, "show-now", "show-now");
        sameRuleForBoth(BACKGROUND, false, false, ScreensaverRules.Player.HOMEBOARD, "show-now", "show-now");
    }

    @Test public void anAppWithAPreviewScreenPlaysItInHomeboardsTaskFromBoth() {
        sameRuleForBoth(WF_CLOCKS, true, true, ScreensaverRules.Player.APP_SCREEN, "play-on-home", "intent-to-home");
    }

    @Test public void anAppWithoutAPreviewScreenUsesAndroidsStartNowFromBoth() {
        sameRuleForBoth(AERIAL, false, true, ScreensaverRules.Player.ANDROID, "android", "android");
    }

    // Google TV has no Somnambulator: Show now plays instead of nothing.
    @Test public void withoutAndroidsStartNowShortcutBothFallBackToShowNow() {
        sameRuleForBoth(AERIAL, false, false, ScreensaverRules.Player.HOMEBOARD, "show-now", "show-now");
    }

    // The firmware is asked about its shortcut only when Android's start-now would be used.
    @Test public void theShortcutIsAskedOnlyForAnAndroidOnlyApp() {
        boolean[] asked = {false};
        RemoteKeys.startScreensaver(CLOCK, false, () -> { asked[0] = true; return true; }, new Recorder("x"));
        RemoteKeys.startScreensaver(WF_CLOCKS, true, () -> { asked[0] = true; return true; }, new Recorder("x"));
        assertEquals(false, asked[0]);
        RemoteKeys.startScreensaver(AERIAL, false, () -> { asked[0] = true; return true; }, new Recorder("x"));
        assertTrue(asked[0]);
    }
}
