package me.goverse.blelinker.server.ancs.notification;


import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author gaoyu
 */
public class Notification implements Parcelable {
    public Notification(NotificationContent content, byte eventId){
        this.content = content;
        long now = System.currentTimeMillis();
        id = content.getId() ^ (int) now;
        addedTime = now;
        this.eventId = eventId;
    }
    private int id;
    private NotificationContent content;
    private long addedTime;
    private boolean isSending;
    private byte eventId;

    public byte getEventId() {
        return eventId;
    }

    public int getId() {
        return id;
    }

    public NotificationContent getContent() {
        return content;
    }

    public long getAddedTime() {
        return addedTime;
    }

    public boolean isSending() {
        return isSending;
    }

    public void setSending(boolean isSending) {
        this.isSending = isSending;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id);
        dest.writeParcelable(this.content, flags);
        dest.writeLong(this.addedTime);
        dest.writeByte(this.isSending ? (byte) 1 : (byte) 0);
        dest.writeByte(this.eventId);
    }

    protected Notification(Parcel in) {
        this.id = in.readInt();
        this.content = in.readParcelable(NotificationContent.class.getClassLoader());
        this.addedTime = in.readLong();
        this.isSending = in.readByte() != 0;
        this.eventId = in.readByte();
    }

    public static final Creator<Notification> CREATOR = new Creator<Notification>() {
        @Override
        public Notification createFromParcel(Parcel source) {
            return new Notification(source);
        }

        @Override
        public Notification[] newArray(int size) {
            return new Notification[size];
        }
    };
}
