package me.goverse.blelinker.client;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import me.goverse.blelinker.BtDevice;
import me.goverse.blelinker.scan.BLEScanner;
import me.goverse.blelinker.utils.BLEUtil;
import me.goverse.blelinker.client.channel.BaseChannel;
import me.goverse.blelinker.client.channel.ChannelAttribute;
import me.goverse.blelinker.client.channel.NotificationChannel;
import me.goverse.blelinker.client.channel.ReadChannel;
import me.goverse.blelinker.client.channel.WriteChannel;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author gaoyu
 */

public class BLEClient implements IBLEClient {

    /**
     * 当前蓝牙连接状态
     */
    private State mCurrentState = State.DISCONNECTED;

    private static final String TAG = "bleClient";

    private Context mContext = null;

    private BluetoothManager mBluetoothManager = null;

    private BluetoothAdapter mBluetoothAdapter = null;

    private BluetoothGatt mBluetoothGatt = null;

    private boolean mIsInitSuccess = false;

    /**
     * 是否已经发现服务(暂时不支持连接后动态添加Channel)
     */
    private boolean mHasDiscoverServices = false;

    private List<IConnectionListener> mConnectionListeners = new ArrayList<>();

    private List<IStateListener> mStateListeners = new ArrayList<>();

    private Handler mHandler = new Handler(Looper.getMainLooper());

    private ArrayMap<ChannelAttribute, BaseChannel> mChannelMap = new ArrayMap<>();

    private List<BaseChannel> mNotifyChannels = new ArrayList<>();

    private BLEScanner mBleScanner;

    /**
     * 当前连接的设备MacAddress
     */
    private String mCurrMacAddress;

    public static class Singleton {
        private static BLEClient mInstance = new BLEClient();
    }

    public static BLEClient getInstance() {
        return Singleton.mInstance;
    }

    public void init(Context context) {
        mContext = context.getApplicationContext();
        registerReceiver();
        mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        mBleScanner = new BLEScanner(context);
    }

    private BLEClient() {

    }


    private boolean checkGattStatusSuccess(int status) {
        if (status == GATT_SUCCESS) return true;
        Log.d(TAG, "checkGattStatusSuccess---status:" + status);
        disconnect();
        releaseBluetoothGatt();
        return false;
    }

    private void resetHasDiscoverServices() {
        mHasDiscoverServices = false;
    }

    /**
     * release BluetoothGatt on mainthread
     */
    private void releaseBluetoothGatt() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mBluetoothGatt != null) {
                    mBluetoothGatt.close();
                    mBluetoothGatt = null;
                }
            }
        });
    }

    private int mRetryCount = 0;

    private void resetRetryCount() {
        mRetryCount = 0;
    }

    private void reConnectDevice(int status, final String macAddress) {
        Log.d(TAG, "retryConnectDevice");
        if (status == GATT_SUCCESS) {
            Log.d(TAG, "connect...");
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mBluetoothGatt != null) {
                        mBluetoothGatt.connect();
                    }
                }
            }, 1000);
        } else {
            releaseBluetoothGatt();
            connectGatt(macAddress, 1000);
        }
    }

    /**
     * retry Connect BtDevice when status != GATT_SUCCESS, maybe 133 or 257
     * @param status status
     * @param macAddress macAddress
     * @return retry result
     */
    private boolean retryConnectDevice(int status, String macAddress) {
        if (mRetryCount < 3) {
            reConnectDevice(status, macAddress);
            mRetryCount ++;
            return true;
        }
        return false;
    }

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.d(TAG, "onConnectionStateChange,status:" + status + ",newState:" + newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (!checkGattStatusSuccess(status)) {
                    notifyOnDisConnected(new BtDevice(gatt.getDevice().getName(), gatt.getDevice().getAddress()));
                    return;
                }
                mCurrentState = State.CONNECTED;
                Log.d(TAG, "STATE_CONNECTED---" + gatt.getDevice().getName() + "---" + gatt.getDevice().getAddress());
                notifyOnConnected(new BtDevice(gatt.getDevice().getName(), gatt.getDevice().getAddress()));
                resetRetryCount();
                if (mBluetoothGatt != null) {
                    mBluetoothGatt.discoverServices();
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mCurrentState = State.DISCONNECTED;
                mIsInitSuccess = false;
                Log.d(TAG, "STATE_DISCONNECTED---" + gatt.getDevice().getName() + "---" + gatt.getDevice().getAddress());
                releaseBluetoothGatt();
                resetHasDiscoverServices();
                notifyOnDisConnected(new BtDevice(gatt.getDevice().getName(), gatt.getDevice().getAddress()));
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.d(TAG, "onServicesDiscovered, status:" + status);
            if (!checkGattStatusSuccess(status)) return;
            mHasDiscoverServices = true;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    setUpChannels();
                }
            });
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
                                         int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.d(TAG, "onCharacteristicRead, status: " + status);
            notifyOnReadListener(status, characteristic.getService().getUuid(), characteristic.getUuid(), characteristic.getValue());
            checkGattStatusSuccess(status);

        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharacteristicWrite,status: " + status);
            notifyOnWriteListener(status, characteristic.getService().getUuid(), characteristic.getUuid());
            checkGattStatusSuccess(status);

        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.d(TAG, "onCharacteristicChanged");
            notifyOnNotificationListener(characteristic.getService().getUuid(), characteristic.getUuid(), characteristic.getValue());
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                     int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            Log.d(TAG, "onDescriptorRead, status:" + status);
            checkGattStatusSuccess(status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                      int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.d(TAG, "onDescriptorWrite, status:" + status);
            ChannelAttribute channelAttribute = mNotifyChannels.get(0).getChannelAttribute();
            if (channelAttribute.getChars().equals(descriptor.getCharacteristic().getUuid()) && (channelAttribute.getDescriptor().equals(descriptor.getUuid()))) {
                boolean res = status == GATT_SUCCESS ? true : false;
                if (res) {
                    mNotifyChannels.remove(0);
                    if (mNotifyChannels.size() == 0) {
                        Log.d(TAG, "notifyOnInitialized");
                        mCurrentState = State.INITIALIZED;
                        mIsInitSuccess = true;
                        notifyOnInitialized(true, new BtDevice(gatt.getDevice().getName(), gatt.getDevice().getAddress()));
                    } else {
                        ChannelAttribute nextChannelAttribute = mNotifyChannels.get(0).getChannelAttribute();
                        setNotificationChannel(nextChannelAttribute.getService(), nextChannelAttribute.getChars(), nextChannelAttribute.getDescriptor());
                    }
                } else {
                    mCurrentState = State.INITIALIZED;
                    mIsInitSuccess = false;
                    notifyOnInitialized(false, null);
                }

            }
            checkGattStatusSuccess(status);
        }

    };

    /**
     * get CurrentState
     * @return State
     */
    @Override
    public State getCurrState() {
        return mCurrentState;
    }

    @Override
    public void startLeScan(BLEScanner.OnScanedListener onScanedListener) {
        mBleScanner.startScan(onScanedListener);
    }

    @Override
    public void startScan(BLEScanner.OnScanedListener onScanedListener) {

        mBleScanner.startScan(onScanedListener);
    }

    @Override
    public void stopScan() {
        mBleScanner.stopScan();
    }

    @Override
    public boolean isInitSuccess() {
        return mIsInitSuccess;
    }

    private void notifyOnReadListener(final int status, final UUID service, final UUID chars, final byte[] value) {
        Log.d(TAG, "notifyOnReadListener");

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                ReadChannel readChannel = (ReadChannel)mChannelMap.get(new ChannelAttribute(service, chars, null));
                boolean res = status == GATT_SUCCESS ? true : false;
                if (readChannel != null && readChannel.getChannelCallBack() != null) {
                    readChannel.getChannelCallBack().onRead(res, value);
                }
            }
        });
    }

    private void notifyOnWriteListener(final int status, final UUID service, final UUID chars) {
        Log.d(TAG, "notifyOnWriteListener");

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                WriteChannel writeChannel = (WriteChannel)mChannelMap.get(new ChannelAttribute(service, chars, null));
                boolean res = status == GATT_SUCCESS ? true : false;
                if (writeChannel != null && writeChannel.getChannelCallBack() != null) {
                    writeChannel.getChannelCallBack().onWrite(res);
                }
            }
        });
    }

    private void notifyOnNotificationListener(final UUID service, final UUID chars, final byte[] value) {
        Log.d(TAG, "notifyOnNotificationListener");

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                NotificationChannel notificationChannel = (NotificationChannel)mChannelMap.get(new ChannelAttribute(service, chars, null));
                if (notificationChannel != null && notificationChannel.getChannelCallBack() != null) {
                    notificationChannel.getChannelCallBack().onNotificationRecieved(value);
                }
            }
        });

    }

    @Override
    public void addConnectionListener(IConnectionListener connectionListener) {
        if (!mConnectionListeners.contains(connectionListener)) {
            mConnectionListeners.add(connectionListener);
        }
    }

    @Override
    public void removeConnectionListener(IConnectionListener connectionListener) {
        if (mConnectionListeners.contains(connectionListener)) {
            mConnectionListeners.remove(connectionListener);
        }
    }

    @Override
    public void addStateListener(IStateListener stateListener) {
        if (!mStateListeners.contains(stateListener)) {
            mStateListeners.add(stateListener);
        }
    }

    @Override
    public void removeStateListener(IStateListener stateListener) {
        if (mStateListeners.contains(stateListener)) {
            mStateListeners.remove(stateListener);
        }
    }

    private void notifyStateChanged(final State state) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                for (IStateListener stateListener : mStateListeners) {
                    if (stateListener != null) stateListener.onStateChanged(state);
                }
            }
        });
    }

    private void notifyOnConnected(final BtDevice btDevice) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                for (IConnectionListener connectionListener : mConnectionListeners) {
                    if (connectionListener != null) connectionListener.onConnected(btDevice);
                }
            }
        });
    }

    private void notifyOnDisConnected(final BtDevice btDevice) {

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                for (IConnectionListener connectionListener : mConnectionListeners) {
                    if (connectionListener != null) connectionListener.onDisConnected(btDevice);
                }
            }
        });
    }

    private void notifyOnInitialized(final boolean res, final BtDevice btDevice) {
        Log.d(TAG, "notifyOnInitialized:" + res);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                for (IConnectionListener connectionListener : mConnectionListeners) {
                    if (connectionListener != null) connectionListener.onInitialized(res, btDevice);
                }
            }
        });
    }

    private boolean checkBluetoothState() {
        return BLEUtil.getBluetoothState() == BLEUtil.STATE_TURNING_ON || BLEUtil.getBluetoothState() == BLEUtil.STATE_TURNING_OFF;
    }

    @Override
    public void connect(final String macAddress) {

        Log.d(TAG, "connect:" + macAddress);
        if (checkBluetoothState()) {
            Log.d(TAG, "Bluetooth state TURNING_ON or TURNING_OFF can not connect");
            return;
        }
        if (mBluetoothAdapter == null) {
            Log.d(TAG, "BluetoothAdapter not initialized.");
            return;
        }

        if (!TextUtils.isEmpty(mCurrMacAddress) && mCurrMacAddress.equalsIgnoreCase(macAddress)) {
            if (mCurrentState == State.CONNECTED || mCurrentState == State.CONNECTING) {
                Log.d(TAG, "reconnect the same device which has established connection with or tring to connect.");
                return;
            }
        } else if (!TextUtils.isEmpty(mCurrMacAddress) && !mCurrMacAddress.equalsIgnoreCase(macAddress)){
            if (mCurrentState == State.CONNECTED || mCurrentState == State.CONNECTING) {
                Log.d(TAG, "connect the device which has established connection with or tring to connect.");
                disconnect();
            }
        }

        releaseBluetoothGatt();
        connectGatt(macAddress);
    }

    private void connectGatt(final String macAddress) {
        connectGatt(macAddress, 0);
    }

    /**
     * connectGatt on mainThread
     * @param macAddress macAddress
     */
    private void connectGatt(final String macAddress, int delayMs) {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(checkNotNull(macAddress));
                if (device != null) {
                    Log.d(TAG, "connectGatt..." + macAddress);
                    mCurrentState = State.CONNECTING;
                    mCurrMacAddress = macAddress;
                    mBluetoothGatt = device.connectGatt(mContext, false, mGattCallback);
                }
            }
        }, delayMs);
    }

    @Override
    public void disconnect() {

        Log.d(TAG, "disconnect");
        if (checkBluetoothState()) {
            Log.d(TAG, "Bluetooth state TURNING_ON or TURNING_OFF can not disconnect");
            return;
        }
        if (mBluetoothGatt != null) {
            resetHasDiscoverServices();
            disConnectGatt();
        }

    }

    /**
     * disConnectGatt on mainThread
     */
    private void disConnectGatt() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mBluetoothGatt.disconnect();
            }
        });
    }

    public boolean refreshGatt() {
        try {
            final Method refresh = BluetoothGatt.class.getMethod("refresh");
            if (refresh != null) {
                boolean success = (Boolean) refresh.invoke(mBluetoothGatt);
                Log.i(TAG, "refreshGatt, is success:  " + success);
                return success;
            }
        } catch (Exception e) {
            Log.i(TAG, "exception occur while refreshing gatt: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }


    /**
     * setUpChannels
     */
    private void setUpChannels() {
        Log.d(TAG, "start to setUp Channels...");

        if (mNotifyChannels.size() != 0) mNotifyChannels.clear();

        Collection<BaseChannel> mChannelList = mChannelMap.values();
        for (BaseChannel channel : mChannelList) {
            ChannelAttribute channelAttribute = channel.getChannelAttribute();
            if (channel.getChannelType() == BaseChannel.ChannelType.WRITE) {
                ((WriteChannel) channel).setBluetoothGatt(mBluetoothGatt);
                setWriteChannel(channelAttribute.getService(), channelAttribute.getChars());
            } else if (channel.getChannelType() == BaseChannel.ChannelType.NOTIFICATION) {
                mNotifyChannels.add(channel);
            } else {
                ((ReadChannel) channel).setBluetoothGatt(mBluetoothGatt);
            }

        }

        if (mNotifyChannels.size() == 0) {
            mCurrentState = State.INITIALIZED;
            mIsInitSuccess = true;
            notifyOnInitialized(true, new BtDevice(mBluetoothGatt.getDevice().getName(), mBluetoothGatt.getDevice().getAddress()));
        }
        else {
            ChannelAttribute channelAttribute = mNotifyChannels.get(0).getChannelAttribute();
            setNotificationChannel(channelAttribute.getService(), channelAttribute.getChars(), channelAttribute.getDescriptor());
        }

    }

    private void setWriteChannel(UUID service, UUID chars) {

        Log.d(TAG, "setWriteChannel,service:" + service.toString() + "---chars:" +chars.toString());
        BluetoothGattService gattService;
        BluetoothGattCharacteristic characteristic;
        if (mBluetoothGatt != null) {
            gattService = mBluetoothGatt.getService(checkNotNull(service, "Service UUID may not be null"));
            if (gattService != null) {
                characteristic = gattService.getCharacteristic(checkNotNull(chars, "characteristic UUID may not be null"));
                if (characteristic != null) {
                    characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                }
            }
        }

    }

    private void setNotificationChannel(UUID service, UUID chars, UUID des) {
        Log.d(TAG, "setNotificationChannel,service:" + service.toString() + "---chars:" +chars.toString() + "---des:" + des.toString());
        BluetoothGattService gattService;
        BluetoothGattCharacteristic characteristic;
        BluetoothGattDescriptor descriptor;
        if (mBluetoothGatt != null) {
            gattService = mBluetoothGatt.getService(checkNotNull(service, "Service UUID may not be null"));
            if (gattService != null) {
                characteristic =gattService.getCharacteristic(checkNotNull(chars, "characteristic UUID may not be null"));
                mBluetoothGatt.setCharacteristicNotification(characteristic, true);
                if (characteristic != null) {
                    descriptor = characteristic.getDescriptor(checkNotNull(des, "descriptor UUID may not be null"));
                    if (descriptor != null) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        mBluetoothGatt.writeDescriptor(descriptor);
                        return;
                    }
                }
            }
        }
        notifyOnInitialized(false, null);
    }

    @Override
    public void addNotificationChannel(UUID service, UUID chars, UUID des, NotificationChannel.OnNotificationListener onNotificationListener) {
        Log.d(TAG, "addNotificationChannel,service:" + service.toString() + "---chars:" +chars.toString() + "---des:" + des.toString());
        ChannelAttribute channelAttribute = new ChannelAttribute(service, chars, des);
        BaseChannel notificationChannel = new NotificationChannel(channelAttribute, onNotificationListener);
        if (!mChannelMap.containsKey(channelAttribute)) {
            mChannelMap.put(channelAttribute, notificationChannel);
        }
        if (mHasDiscoverServices) {
            setNotificationChannel(service, chars, des);
        }
    }

    public void release() {
        mChannelMap.clear();
        mHasDiscoverServices = false;
        mContext.unregisterReceiver(mStateReceiver);
    }

    @Override
    public ReadChannel addReadChannel(UUID service, UUID chars) {
        Log.d(TAG, "addReadChannel,service:" + service.toString() + "---chars:" +chars.toString());
        ChannelAttribute channelAttribute = new ChannelAttribute(service, chars, null);
        ReadChannel readChannel = new ReadChannel(channelAttribute);
        if (!mChannelMap.containsKey(channelAttribute)) {
            mChannelMap.put(channelAttribute, readChannel);
        }
        if (mHasDiscoverServices) {
            readChannel.setBluetoothGatt(mBluetoothGatt);
        }
        return readChannel;
    }

    @Override
    public WriteChannel addWriteChannel(UUID service, UUID chars) {
        Log.d(TAG, "addWriteChannel,service:" + service.toString() + "---chars:" +chars.toString());
        ChannelAttribute channelAttribute = new ChannelAttribute(service, chars, null);
        WriteChannel writeChannel = new WriteChannel(channelAttribute);
        if (!mChannelMap.containsKey(channelAttribute)) {
            mChannelMap.put(channelAttribute, writeChannel);
        }
        if (mHasDiscoverServices) {
            writeChannel.setBluetoothGatt(mBluetoothGatt);
            setWriteChannel(service, chars);
        }
        return writeChannel;
    }


    @Override
    public void removeChannel(UUID service, UUID chars) {
        Log.d(TAG, "removeChannel,service:" + service.toString() + "---chars:" +chars.toString());
        mChannelMap.remove(new ChannelAttribute(service, chars, null));
    }

    @Override
    public void clearChannels() {
        mChannelMap.clear();
        mNotifyChannels.clear();
    }

    private void registerReceiver() {
        IntentFilter statusFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        mContext.registerReceiver(mStateReceiver, statusFilter);
    }

    private BroadcastReceiver mStateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            switch(intent.getAction()){
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                    mIsInitSuccess = false;
                    switch(blueState){
                        case BluetoothAdapter.STATE_TURNING_ON:
                            mCurrentState = State.TURNING_ON;
                            notifyStateChanged(State.TURNING_ON);
                            break;
                        case BluetoothAdapter.STATE_ON:
                            mCurrentState = State.ON;
                            notifyStateChanged(State.ON);
                            break;
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            mCurrentState = State.TURNING_OFF;
                            notifyStateChanged(State.TURNING_OFF);
                            break;
                        case BluetoothAdapter.STATE_OFF:
                            mCurrentState = State.OFF;
                            notifyStateChanged(State.OFF);
                            break;
                    }
                    break;
            }
        }
    };

}
