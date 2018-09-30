package me.goverse.blelinker.client.channel;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author gaoyu 2017/9/6
 */

public class WriteChannel extends BaseChannel<WriteChannel.OnWriteListener>{

    public interface OnWriteListener {
        void onWrite(boolean res);
    }

    private BluetoothGatt mBluetoothGatt;

    private OnWriteListener mOnWriteListener;

    @Override
    public OnWriteListener getChannelCallBack() {
        return mOnWriteListener;
    }

    public WriteChannel(ChannelAttribute channelAttribute) {
        super(checkNotNull(channelAttribute));
    }

    public void setBluetoothGatt(BluetoothGatt bluetoothGat) {
        this.mBluetoothGatt = checkNotNull(bluetoothGat);
    }

    public boolean write(byte[] data, OnWriteListener onWriteListener) {
        mOnWriteListener = onWriteListener;
        BluetoothGattService service = checkNotNull(mBluetoothGatt,"please connect device first!!!").getService(mChannelAttribute.getService());
        if (service == null && mOnWriteListener != null) {
            mOnWriteListener.onWrite(false);
            return false;
        }
        BluetoothGattCharacteristic chars = service.getCharacteristic(mChannelAttribute.getChars());
        if (chars == null && mOnWriteListener != null) {
            mOnWriteListener.onWrite(false);
            return false;
        }
        chars.setValue(data);
        return mBluetoothGatt.writeCharacteristic(chars);
    }

    @Override
    public ChannelType getChannelType() {
        return ChannelType.WRITE;
    }
}
