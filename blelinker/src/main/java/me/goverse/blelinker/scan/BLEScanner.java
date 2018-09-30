package me.goverse.blelinker.scan;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import me.goverse.blelinker.BtDevice;
import me.goverse.blelinker.utils.BLEUtil;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY;

/**
 * @author gaoyu
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class BLEScanner {

    private boolean mIsScanning = false;

    private boolean mUseLeScan = false;

    /**
     * 设备扫描回调
     */
    public interface OnScanedListener {
        /**
         * @param btDevice 设备
         */
        void onScaned(BtDevice btDevice);
    }

    public BLEScanner(Context context){
        mContext = context;
        mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
    }

    private Context mContext = null;

    private BluetoothManager mBluetoothManager = null;

    private BluetoothAdapter mBluetoothAdapter = null;

    private OnScanedListener mOnScanedListener = null;

    private Handler mHandler = new Handler(Looper.getMainLooper());

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice bluetoothDevice, final int i, final byte[] bytes) {
            if(mOnScanedListener != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mOnScanedListener.onScaned(new BtDevice(parseDeviceName(bytes), bluetoothDevice.getAddress(), i));
                    }
                });
            }
        }
    };

    /**
     * 扫描设备(传统蓝牙)
     *@param onScanedListener 扫描结果监听
     */
    public void startScan(OnScanedListener onScanedListener) {
        if (!BLEUtil.checkBluetoothIsOpen()) return;
        mOnScanedListener = onScanedListener;
        mUseLeScan = false;
        registerDiscoverReceiver();
        mBluetoothAdapter.startDiscovery();
    }

    private void registerDiscoverReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        mContext.registerReceiver(mDeviceReceiver, filter);
    }

    private void unRegisterDiscoverReceiver() {
        mContext.unregisterReceiver(mDeviceReceiver);
    }

    private BroadcastReceiver mDeviceReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getType() == BluetoothDevice.DEVICE_TYPE_LE) {
                    short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (short)-60);
                    if(mOnScanedListener != null) mOnScanedListener.onScaned(new BtDevice(device.getName(), device.getAddress(), rssi));
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {

            }
        }
    };

    /**
     * 扫描低功耗设备
     * @param onScanedListener
     */
    public void startLeScan(OnScanedListener onScanedListener) {
        if (!BLEUtil.checkBluetoothIsOpen()) return;
        mOnScanedListener = onScanedListener;
        mIsScanning = true;
        mUseLeScan = true;

        if (checkSDKVersion()) {
            startNewLeScan();
        } else {
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        }
    }

    private boolean checkSDKVersion() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    private void startNewLeScan() {
        BluetoothLeScanner bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                BluetoothDevice device = result.getDevice();
                if(mOnScanedListener != null) mOnScanedListener.onScaned(new BtDevice(device.getName(), device.getAddress(), result.getRssi()));
            }
        };
        ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(SCAN_MODE_LOW_LATENCY);
        bluetoothLeScanner.startScan(null, builder.build(), mScanCallback);
    }

    private void stopNewLeScan() {
        BluetoothLeScanner bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        if (bluetoothLeScanner != null) {
            bluetoothLeScanner.stopScan(mScanCallback);
        }
    }

    private ScanCallback mScanCallback = null;

    /**
     *停止扫描
     */
    public void stopScan() {

        if (!mIsScanning) return;
        mIsScanning = false;
        mOnScanedListener = null;
        if (mUseLeScan) {
            if (checkSDKVersion()) stopNewLeScan();
            else {
                if (mBluetoothAdapter != null) {

                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }
        } else {
            unRegisterDiscoverReceiver();
            mBluetoothAdapter.cancelDiscovery();
        }
    }

    private String parseDeviceName(byte[] data) {
        String name = "";
        List<UUID> uuids = new ArrayList<UUID>();
        if(data == null ){
            return name;
        }
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        while (buffer.remaining() > 2) {
            byte length = buffer.get();
            if (length == 0) break;

            byte type = buffer.get();
            switch (type) {
                case 0x02: // Partial list of 16-bit UUIDs
                case 0x03: // Complete list of 16-bit UUIDs
                    while (length >= 2) {
                        uuids.add(UUID.fromString(String.format(
                                "%08x-0000-1000-8000-00805f9b34fb", buffer.getShort())));
                        length -= 2;
                    }
                    break;
                case 0x06: // Partial list of 128-bit UUIDs
                case 0x07: // Complete list of 128-bit UUIDs
                    while (length >= 16) {
                        long lsb = buffer.getLong();
                        long msb = buffer.getLong();
                        uuids.add(new UUID(msb, lsb));
                        length -= 16;
                    }
                    break;
                case 0x09:
                    byte[] nameBytes = new byte[length-1];
                    buffer.get(nameBytes);
                    try {
                        name = new String(nameBytes, "utf-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    //modified by Leopard 防止骚扰设备越界
                    int newPosition=buffer.position() + length - 1;
                    if(newPosition>=buffer.limit()){
                        return name;
                    }

                    buffer.position(newPosition);
                    break;
            }
        }
        return name;
    }

}
