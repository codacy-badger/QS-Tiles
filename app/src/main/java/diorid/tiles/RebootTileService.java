package diorid.tiles;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class RebootTileService extends TileService {

    public static boolean findBinary(String binaryName) {
        boolean found = false;
        String[] places = {"/sbin/", "/system/bin/", "/system/xbin/",
                "/data/local/xbin/", "/data/local/bin/",
                "/system/sd/xbin/", "/system/bin/failsafe/", "/data/local/"};
        for (String where : places) {
            if (new File(where + binaryName).exists()) {
                found = true;

                break;
            }
        }
        return found;
    }

    private static boolean isRooted() {
        return findBinary("su");
    }

    @Override
    public void onClick() {
        super.onClick();

        try {
            Process proc = Runtime.getRuntime().exec(new String[]{"su", "-c", "reboot"});
            proc.waitFor();
        } catch (Exception ignored) {
        }

    }

    @Override
    public void onTileAdded() {
        super.onTileAdded();

        if (isRooted() && isRootGiven()) {
            getQsTile().setState(Tile.STATE_INACTIVE);
        } else {
            getQsTile().setState(Tile.STATE_UNAVAILABLE);
            Toast.makeText(this, "Root permission needed for this tile", Toast.LENGTH_LONG).show();
        }
    }

    public boolean isRootGiven() {
        if (isRooted()) {
            Process process = null;
            try {
                process = Runtime.getRuntime().exec(new String[]{"su", "-c", "id"});
                BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String output = in.readLine();
                if (output != null && output.toLowerCase().contains("uid=0"))
                    return true;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (process != null)
                    process.destroy();
            }
        }

        return false;
    }
}
