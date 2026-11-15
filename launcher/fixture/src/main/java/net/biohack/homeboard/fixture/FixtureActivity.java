package net.biohack.homeboard.fixture;
public class FixtureActivity extends android.app.Activity {
    @Override public void onCreate(android.os.Bundle state) {
        super.onCreate(state);
        android.widget.TextView view = new android.widget.TextView(this);
        view.setText("Homeboard test app\nSafe to uninstall during coordinator tests\nBack returns to Homeboard");
        view.setTextSize(28); view.setGravity(android.view.Gravity.CENTER);
        setContentView(view);
    }
}
