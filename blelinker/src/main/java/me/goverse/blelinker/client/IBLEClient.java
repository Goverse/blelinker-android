package me.goverse.blelinker.client;

import me.goverse.blelinker.BtDevice;
import me.goverse.blelinker.scan.BLEScanner;
import me.goverse.blelinker.client.channel.NotificationChannel;
import me.goverse.blelinker.client.channel.ReadChannel;
import me.goverse.blelinker.client.channel.WriteChannel;

import java.util.UUID;

/**
 * @author gaoyu
 */

public interface IBLEClient {

    public enum State {
        ON,
        OFF,
        TURNING_ON,
        TURNING_OFF,
        CONNECTING,
        CONNECTED,
        INITIALIZED,
        DISCONNECTED
    }

    public interface IConnectionListener {
        void onConnected(BtDevice btDevice);
        void onDisConnected(BtDevice btDevice);
        void onInitialized(boolean res, BtDevice btDevice);
    }

    public interface IStateListener {
        void onStateChanged(State state);
    }

    void addConnectionListener(IConnectionListener connectionListener);

    void removeConnectionListener(IConnectionListener connectionListener);

    void addStateListener(IStateListener stateListener);

    void removeStateListener(IStateListener stateListener);

    /**
     * connect
     * @param macAddress macAddress
     */
    void connect(String macAddress);

    /**
     * disconnect
     */
    void disconnect();

    /**
     * add NotificationChannel
     */
    void addNotificationChannel(UUID service, UUID chars, UUID des, NotificationChannel.OnNotificationListener onNotificaionListener);

    /**
     * add ReadChannel
     */
    ReadChannel addReadChannel(UUID service, UUID chars);

    /**
     * add WriteChannel
     */
    WriteChannel addWriteChannel(UUID service, UUID chars);

    /**
     * remove Channel
     * @param service service UUID
     * @param chars chars UUID
     */
    void removeChannel(UUID service, UUID chars);

    void clearChannels();

    State getCurrState();

    /**
     * startLeScan
     * @param onScanedListener onScanedListener
     */
    void startLeScan(BLEScanner.OnScanedListener onScanedListener);

    /**
     * startScan
     * @param onScanedListener onScanedListener
     */
    void startScan(BLEScanner.OnScanedListener onScanedListener);

    /**
     * stopScan
     */
    void stopScan();

    boolean isInitSuccess();
}
