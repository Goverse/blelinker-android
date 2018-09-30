package me.goverse.blelinker.server;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;

/**
 * @author gaoyu
 */

public class BLEServer {

    /**
     * ServerListener
     */
    public interface IServerListener {
        /**
         * @param device 设备
         */
        void onConnected(BluetoothDevice device);

        /**
         * @param device 设备
         */
        void onDisConnected(BluetoothDevice device);

        /**
         * A remote client has requested to read a local characteristic.
         * @param device The remote device that has requested the read operation
         * @param requestId The Id of the request
         * @param offset Offset into the value of the characteristic
         * @param characteristic Characteristic to be read
         */
        void onCharacteristicReadRequest(BluetoothDevice device, int requestId,
                                         int offset, BluetoothGattCharacteristic characteristic);

        /**
         * A remote client has requested to write to a local characteristic.
         * @param device The remote device that has requested the write operation
         * @param requestId The Id of the request
         * @param characteristic Characteristic to be written to.
         * @param preparedWrite true, if this write operation should be queued for
         *                      later execution.
         * @param responseNeeded true, if the remote device requires a response
         * @param offset The offset given for the value
         * @param value The value the client wants to assign to the characteristic
         */
        void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                                          BluetoothGattCharacteristic characteristic,
                                          boolean preparedWrite, boolean responseNeeded,
                                          int offset, byte[] value);

        /**
         * A remote client has requested to read a local descriptor.
         * @param device The remote device that has requested the read operation
         * @param requestId The Id of the request
         * @param offset Offset into the value of the characteristic
         * @param descriptor Descriptor to be read
         */
        void onDescriptorReadRequest(BluetoothDevice device, int requestId,
                                     int offset, BluetoothGattDescriptor descriptor);

        /**
         * A remote client has requested to write to a local descriptor.
         * @param device The remote device that has requested the write operation
         * @param requestId The Id of the request
         * @param descriptor Descriptor to be written to.
         * @param preparedWrite true, if this write operation should be queued for
         *                      later execution.
         * @param responseNeeded true, if the remote device requires a response
         * @param offset The offset given for the value
         * @param value The value the client wants to assign to the descriptor
         */
        void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                                      BluetoothGattDescriptor descriptor,
                                      boolean preparedWrite, boolean responseNeeded,
                                      int offset, byte[] value);

    }

    private Context mContext = null;

    private BluetoothManager mBluetoothManager = null;

    private BluetoothGattServer mBluetoothGattServer;

    private List<IServerListener> mServerListeners = new ArrayList<>();

    private Handler mHandler = new Handler(Looper.getMainLooper());

    /**
     * addServerListener
     * @param serverListener serverListener
     */
    public void addServerListener(IServerListener serverListener) {
        if (!mServerListeners.contains(serverListener)) {
            mServerListeners.add(serverListener);
        }
    }

    /**
     * removeServerListener
     * @param serverListener serverListener
     */
    public void removeServerListener(IServerListener serverListener) {
        if (mServerListeners.contains(serverListener)) {
            mServerListeners.remove(serverListener);
        }
    }

    private BluetoothGattServerCallback mBluetoothGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    notifyOnConnected(device);
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    notifyOnDisConnected(device);
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);

            notifyOnCharacteristicReadRequest(device, requestId, offset, characteristic);
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device,
                                                 int requestId,
                                                 BluetoothGattCharacteristic characteristic,
                                                 boolean preparedWrite,
                                                 boolean responseNeeded,
                                                 int offset,
                                                 byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            notifyOnCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);

            notifyOnDescriptorReadRequest(device, requestId, offset, descriptor);
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device,
                                             int requestId,
                                             BluetoothGattDescriptor descriptor,
                                             boolean preparedWrite,
                                             boolean responseNeeded,
                                             int offset,
                                             byte[] value) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);

            notifyOnDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            super.onServiceAdded(status, service);
        }

    };

    public BLEServer(Context context) {
        mContext = context;
        mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
    }

    /**
     * 打开服务
     */
    public void openServer() {
        mBluetoothGattServer = mBluetoothManager.openGattServer(mContext, mBluetoothGattServerCallback);
    }

    /**
     * 关闭服务
     */
    public void closeServer() {
        if(mBluetoothGattServer != null){
            mBluetoothGattServer.clearServices();
            mBluetoothGattServer.close();
            mBluetoothGattServer = null;
        }
    }

    /**
     * 连接设备
     * @param macAddress 设备mac
     * @param autoConnect 自动连接
     */
    public void connect(String macAddress, boolean autoConnect) {

        BluetoothDevice device =
                BluetoothAdapter.getDefaultAdapter().getRemoteDevice(macAddress);
        mBluetoothGattServer.connect(device, autoConnect);
    }

    /**
     * 断开连接
     * @param macAddress 设备mac
     */
    public void disconnect(String macAddress){
        if(mBluetoothGattServer != null){
            BluetoothDevice device =
                    BluetoothAdapter.getDefaultAdapter().getRemoteDevice(macAddress);
            mBluetoothGattServer.cancelConnection(device);
        }
    }

    /**
     * 添加服务
     * @param customService customService
     */
    public void addService(CustomService customService) {
        if (mBluetoothGattServer != null) {
            mBluetoothGattServer.addService(customService);
            customService.setBleServer(this);
            addServerListener(customService);
        }
    }

    private void notifyOnConnected(final BluetoothDevice device) {

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                for (IServerListener serverListener : mServerListeners) {
                    serverListener.onConnected(device);
                }
            }
        });
    }

    private void notifyOnDisConnected(final BluetoothDevice device) {

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                for (IServerListener serverListener : mServerListeners) {
                    serverListener.onDisConnected(device);
                }
            }
        });
    }

    private void notifyOnCharacteristicReadRequest(final BluetoothDevice device, final int requestId,
                                                   final int offset, final BluetoothGattCharacteristic characteristic) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                for (IServerListener serverListener : mServerListeners) {
                    serverListener.onCharacteristicReadRequest(device, requestId, offset, characteristic);
                }
            }
        });
    }

    private void notifyOnCharacteristicWriteRequest(final BluetoothDevice device, final int requestId,
                                                    final BluetoothGattCharacteristic characteristic,
                                                    final boolean preparedWrite, final boolean responseNeeded,
                                                    final int offset, final byte[] value) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                for (IServerListener serverListener : mServerListeners) {
                    serverListener.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
                }
            }
        });
    }

    private void notifyOnDescriptorReadRequest(final BluetoothDevice device, final int requestId,
                                               final int offset, final BluetoothGattDescriptor descriptor) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                for (IServerListener serverListener : mServerListeners) {
                    serverListener.onDescriptorReadRequest(device, requestId, offset, descriptor);
                }
            }
        });
    }

    private void notifyOnDescriptorWriteRequest(final BluetoothDevice device, final int requestId,
                                                final BluetoothGattDescriptor descriptor,
                                                final boolean preparedWrite, final boolean responseNeeded,
                                                final int offset, final byte[] value) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                for (IServerListener serverListener : mServerListeners) {
                    serverListener.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
                }
            }
        });
    }


    /**
     * 发送通知
     * @param device 设备
     * @param characteristic 特征值
     * @param confirm true to request confirmation from the client (indication),
     *                false to send a notification
     */
    public void notifyCharacteristicChanged(BluetoothDevice device, BluetoothGattCharacteristic characteristic, boolean confirm) {

        mBluetoothGattServer.notifyCharacteristicChanged(device, characteristic, confirm);
    }

    /**
     * Send a response to a read or write request to a remote device.
     * @param device The remote device to send this response to
     * @param requestId The ID of the request that was received with the callback
     * @param status The status of the request to be sent to the remote devices
     * @param offset Value offset for partial read/write response
     * @param value The value of the attribute that was read/written (optional)
     */
    public void sendResponse(BluetoothDevice device, int requestId,
                             int status, int offset, byte[] value) {
        mBluetoothGattServer.sendResponse(device, requestId, status, offset, value);
    }

}
