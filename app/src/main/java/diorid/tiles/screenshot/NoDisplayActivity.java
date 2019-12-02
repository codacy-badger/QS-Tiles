package diorid.tiles.screenshot;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import static diorid.tiles.BuildConfig.APPLICATION_ID;
import static diorid.tiles.screenshot.UtilsKt.screenshot;

public class NoDisplayActivity extends Activity {

    private static final String EXTRA_SCREENSHOT = APPLICATION_ID + ".NoDisplayActivity.EXTRA_SCREENSHOT";

    public static Intent newIntent(Context context, boolean screenshot) {
        Intent intent = new Intent(context, NoDisplayActivity.class);
        intent.putExtra(EXTRA_SCREENSHOT, screenshot);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent != null) {
            String action = intent.getAction();
            if (intent.getBooleanExtra(EXTRA_SCREENSHOT, false) || (action != null && action.equals(EXTRA_SCREENSHOT))) {
                screenshot(this);
            }
        }
        finish();
    }
}
