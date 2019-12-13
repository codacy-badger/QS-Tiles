package diorid.tiles.screenshot;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import static diorid.tiles.BuildConfig.APPLICATION_ID;
import static diorid.tiles.screenshot.UtilKt.screenshot;

public class NonDisplayActivity extends Activity {

    private static final String EXTRA_SCREENSHOT = APPLICATION_ID + ".NonDisplayActivity.EXTRA_SCREENSHOT";
    private static final String EXTRA_PARTIAL = APPLICATION_ID + ".NonDisplayActivity.EXTRA_PARTIAL";

    public static Intent newIntent(Context context, boolean screenshot) {
        Intent intent = new Intent(context, NonDisplayActivity.class);
        intent.putExtra(EXTRA_SCREENSHOT, screenshot);
        return intent;
    }

    public static Intent newPartialIntent(Context context) {
        Intent intent = new Intent(context, NonDisplayActivity.class);
        intent.putExtra(EXTRA_PARTIAL, true);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent != null) {
            String action = intent.getAction();

            if (intent.getBooleanExtra(EXTRA_PARTIAL, false)) {
                screenshot(this, true);
            } else if (intent.getBooleanExtra(EXTRA_SCREENSHOT, false) || (action != null && action.equals(EXTRA_SCREENSHOT))) {
                screenshot(this, false);
            }
        }
        finish();
    }
}
