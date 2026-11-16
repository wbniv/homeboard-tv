package com.gathin.tvhome;
/** Opens the actual All Apps surface for coordinator visual checks. No settings are changed here. */
public final class AllAppsProbeActivity extends MainActivity {
 @Override protected void onCreate(android.os.Bundle state) {
  getIntent().setAction(ACTION_APP_LIBRARY); super.onCreate(state);
 }
}
