package me.goverse.blelinker.server.ancs;

import me.goverse.blelinker.server.ancs.constant.AncsID;
import me.goverse.blelinker.server.ancs.constant.AncsOffset;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author gaoyu
 */

public class AncsCommand {

    private byte[] value;

    private byte commandId;

    private int notificationUID;

    private Map<Byte, Short> attributeMap;

    private List<Map.Entry<Byte, Short>> attributes = new ArrayList<>();

    public byte[] getValue() {
        return value;
    }

    public void setValue(byte[] value) {
        this.value = value;
    }

    public byte getCommandId() {
        return commandId;
    }

    public void setCommandId(byte commandId) {
        this.commandId = commandId;
    }

    public int getNotificationUID() {
        return notificationUID;
    }

    public void setNotificationUID(int notificationUID) {
        this.notificationUID = notificationUID;
    }

    public Map<Byte, Short> getAttributeMap() {
        return attributeMap;
    }

    public void setAttributeMap(Map<Byte, Short> attributeMap) {
        this.attributeMap = attributeMap;
    }

    public List<Map.Entry<Byte, Short>> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<Map.Entry<Byte, Short>> attributes) {
        this.attributes = attributes;
    }

    public AncsCommand(byte[] value) {
        this.value = value;
        this.commandId = value[AncsOffset.GAA.COMMANDID];
        this.notificationUID = ByteBuffer.wrap(value, 1, 4)
                .order(ByteOrder.LITTLE_ENDIAN).getInt();
        Map<Byte, Short> map = attributeMap = new HashMap<>();
        for (int i = 5; i < value.length; ) {
            switch (value[i]) {
                case AncsID.NotificationAttribute.MESSAGE_SIZE:
                case AncsID.NotificationAttribute.DATE:
                    map.put(value[i], (short) -1);
                    attributes.add(new AbstractMap.SimpleEntry<Byte, Short>(value[i], (short) -1));
                    ++i;
                    break;
                case AncsID.NotificationAttribute.APP_IDENTIFIER:
                    int length = 0;
                    int j = i + 1;
                    for (; j < value.length; j++) {
                        int temp = value[j] & 0xFF;
                        if(temp > 0 && temp < 8) {
                            break;
                        }
                        length |= (temp << (8 * (j - i - 1)));
                    }
                    if(length == 0){
                        length = 0x20;
                    }
                    attributes.add(new AbstractMap.SimpleEntry<>(value[i], (short) length));
                    i = j;
                    break;
                case AncsID.NotificationAttribute.TITLE:
                case AncsID.NotificationAttribute.SUBTITLE:
                case AncsID.NotificationAttribute.MESSAGE:
                    map.put(value[i], ByteBuffer.wrap(value, i + 1, 2).order(ByteOrder.LITTLE_ENDIAN).getShort());
                    attributes.add(new AbstractMap.SimpleEntry<Byte, Short>(value[i],
                            ByteBuffer.wrap(value, i + 1, 2).order(ByteOrder.LITTLE_ENDIAN).getShort()));
                    i += 3;
                    break;

                default:
                    ++i;
                    break;
            }
        }
    }

    /**
     * getResponseLen
     * @return int
     */
    public int getResponseLen() {
        int len = 1 + 4;
        for (int i = 0; i < attributes.size(); i++) {
            Map.Entry<Byte, Short> entry = attributes.get(i);
            len += 1;
            if (entry.getValue() > 0) {
                len += 2;
                len += entry.getValue();
            } else {
                len += 4;
            }
        }
        return len;
    }
}
