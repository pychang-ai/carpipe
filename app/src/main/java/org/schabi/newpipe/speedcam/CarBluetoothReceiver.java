package org.schabi.newpipe.speedcam;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

/**
 * Starts and stops driving mode with the car stereo.
 *
 * <p>Connecting to the car is the one moment that reliably means "a drive is starting",
 * so the warning turns itself on then and off again on the way out. That way the driver
 * never has to remember a switch, and the position is not being followed at other times.
 */
public class CarBluetoothReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(@NonNull final Context context, @NonNull final Intent intent) {
        if (!SpeedCameraSettings.isEnabled(context)) {
            return;
        }

        final String action = intent.getAction();
        if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
            SpeedCameraService.setRunning(context, true);
        } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
            SpeedCameraService.setRunning(context, false);
        }
    }
}
