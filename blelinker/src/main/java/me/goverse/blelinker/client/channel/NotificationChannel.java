package me.goverse.blelinker.client.channel;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author gaoyu 2017/9/6
 */

public class NotificationChannel extends BaseChannel<NotificationChannel.OnNotificationListener>{

    public interface OnNotificationListener {
        public void onNotificationRecieved(byte[] data);
    }

    private OnNotificationListener onNotificationListener;

    public NotificationChannel(ChannelAttribute channelAttribute, OnNotificationListener onNotificationListener) {
        super(checkNotNull(channelAttribute));
        this.onNotificationListener = checkNotNull(onNotificationListener);
    }

    @Override
    public OnNotificationListener getChannelCallBack() {
        return onNotificationListener;
    }

    @Override
    public ChannelType getChannelType() {
        return ChannelType.NOTIFICATION;
    }
}
