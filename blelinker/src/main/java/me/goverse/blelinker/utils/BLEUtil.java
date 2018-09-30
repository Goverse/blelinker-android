package me.goverse.blelinker.utils;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Settings;

/**
 * @author gaoyu 2017/9/13
 */

public class BLEUtil {

    public static final int STATE_OFF = 0;
    public static final int STATE_TURNING_ON = 1;
    public static final int STATE_ON = 2;
    public static final int STATE_TURNING_OFF = 3;

    /**
     * check Bluetooth Supported
     * @return true:support, false:not support
     */
    public static boolean checkBluetoothSupported() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        return adapter != null;
    }

    /**
     * check Bluetooth Is Open
     * @return true:open, false:not open
     */
    public static boolean checkBluetoothIsOpen() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        return adapter != null && adapter.isEnabled();
    }

    /**
     * check BLE Supported
     * @return true:support, false:not support
     */
    public static boolean checkBLESupported(Context context) {

       return context.getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    /**
     * get Bluetooth State
     * @return Bluetooth State
     */
    public static int getBluetoothState() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        int state = STATE_OFF;
        if (adapter.getState() == BluetoothAdapter.STATE_ON) {
            state = STATE_ON;
        } else if (adapter.getState() == BluetoothAdapter.STATE_OFF) {
            state = STATE_OFF;
        } else if (adapter.getState() == BluetoothAdapter.STATE_TURNING_ON) {
            state = STATE_TURNING_ON;
        } else if (adapter.getState() == BluetoothAdapter.STATE_TURNING_OFF) {
            state = STATE_TURNING_OFF;
        }
        return state;
    }

    /**
     * force Open Bluetooth
     * @return open result true:open success, false:open failed
     */
    public static boolean forceOpenBluetooth() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            return false;
        }
        return adapter.enable();
    }

    /**
     * force Close Bluetooth
     * @return Close result true:close success, false:close failed
     */
    public static boolean forceCloseBluetooth() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            return false;
        }
        return adapter.disable();
    }

    /**
     * request Open Bluetooth
     */
    public static void requestOpenBluetooth(Activity activity, int requestCode) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if(adapter == null){
            return;
        }
        if(!adapter.isEnabled()){
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableIntent, requestCode);
        }
    }

    /**
     * request Bluetooth Discoverable
     */
    public static void requestBluetoothDiscoverable(Activity activity) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            activity.startActivity(discoverableIntent);
        }
    }

    /**
     * goto Setting and then user can open or close bluetooth
     */
    public static void gotoBluetoothSetting(Context context) {
        context.startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
    }
}
