package me.goverse.blelinker.client.channel;

import java.util.UUID;

/**
 * @author gaoyu 2017/9/6
 */

public class ChannelAttribute {

    public ChannelAttribute(UUID service, UUID chars, UUID descriptor) {

        this.service = service;
        this.chars = chars;
        this.descriptor = descriptor;
    }

    private UUID service;
    private UUID chars;
    private UUID descriptor;

    public UUID getService() {
        return service;
    }

    public void setService(UUID service) {
        this.service = service;
    }

    public UUID getChars() {
        return chars;
    }

    public void setChars(UUID chars) {
        this.chars = chars;
    }

    public UUID getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(UUID descriptor) {
        this.descriptor = descriptor;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + service.hashCode();
        result = 31 * result + chars.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != ChannelAttribute.class) {
            return false;
        }
        ChannelAttribute channelAttribute = (ChannelAttribute) obj;
        if (channelAttribute.getService() == null) {
            if (channelAttribute.getService() != getService()) return false;
        } else {
            if (!channelAttribute.getService().equals(getService())) return false;
        }
        if (channelAttribute.getChars() == null) {
            if (channelAttribute.getChars() != getChars()) return false;
        } else {
            if (!channelAttribute.getChars().equals(getChars())) return false;
        }
        return true;
    }
}
