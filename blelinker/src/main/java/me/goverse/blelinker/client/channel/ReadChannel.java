package me.goverse.blelinker.client.channel;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author gaoyu 2017/9/6
 */

public class ReadChannel extends BaseChannel<ReadChannel.OnReadListener> {

    public interface OnReadListener {
        public void onRead(boolean res, byte[] data);
    }

    private BluetoothGatt mBluetoothGatt;

    private OnReadListener onReadListener;

    @Override
    public OnReadListener getChannelCallBack() {
        return onReadListener;
    }

    public ReadChannel(ChannelAttribute channelAttribute) {
        super(checkNotNull(channelAttribute));
    }

    public void setBluetoothGatt(BluetoothGatt bluetoothGatt) {
        this.mBluetoothGatt = checkNotNull(bluetoothGatt);
    }

    public boolean read(OnReadListener onReadListener) {
        this.onReadListener = onReadListener;
        BluetoothGattService service = checkNotNull(mBluetoothGatt,"please connect device first!!!").getService(mChannelAttribute.getService());
        if (service == null && onReadListener != null) {
            onReadListener.onRead(false, null);
            return false;
        }
        BluetoothGattCharacteristic chars = service.getCharacteristic(mChannelAttribute.getChars());
        if (chars == null && onReadListener != null) {
            onReadListener.onRead(false, null);
            return false;
        }
        return mBluetoothGatt.readCharacteristic(chars);
    }

    @Override
    public ChannelType getChannelType() {
        return ChannelType.READ;
    }

    private Runnable mTimeOut = new Runnable() {
        @Override
        public void run() {
            if (onReadListener != null) {
                onReadListener = null;
            }
        }
    };


}
