package me.goverse.blelinker.client.channel;


/**
 * @author gaoyu 2017/9/6
 */

public abstract class BaseChannel<T> {

    public enum ChannelType {
        READ,
        WRITE,
        NOTIFICATION
    }

    public abstract T getChannelCallBack();

    protected ChannelAttribute mChannelAttribute;

    public BaseChannel(ChannelAttribute channelAttribute) {
        mChannelAttribute = channelAttribute;
    }

    public ChannelAttribute getChannelAttribute() {
        return mChannelAttribute;
    }

    public abstract ChannelType getChannelType();

}