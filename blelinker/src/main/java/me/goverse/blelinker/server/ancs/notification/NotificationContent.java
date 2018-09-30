package me.goverse.blelinker.server.ancs.notification;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author gaoyu
 */
public final class NotificationContent implements Parcelable {

	private int id;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public byte getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(byte categoryId) {
		this.categoryId = categoryId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public static Creator<NotificationContent> getCREATOR() {
		return CREATOR;
	}

	private byte categoryId;
	private String title;
	private String message;
	private String packageName;
	
	public NotificationContent(){
		
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if(o == null || !(o instanceof NotificationContent)){
			return false;
		}
		NotificationContent other = (NotificationContent) o;
		if(categoryId != other.categoryId || !packageName.equals(other.packageName)
				|| !title.equals(other.title) || !message.equals(other.message)){
			return false;
		}

		if (id != other.id || packageName == null || title == null || message == null) {
			return false;
		}

		return true;
	}
	
	private NotificationContent(Parcel in){
		id = in.readInt();
		categoryId = in.readByte();
		title = in.readString();
		message = in.readString();
		packageName = in.readString();
	}
	
	@Override
	public int describeContents() {
		return 0;
	}
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(id);
		dest.writeByte(categoryId);
		dest.writeString(title);
		dest.writeString(message);
		dest.writeString(packageName);		
	}

	/**
	 * CREATOR
	 */
    public static final Creator<NotificationContent> CREATOR =
    		new Creator<NotificationContent>() {
		public NotificationContent createFromParcel(Parcel in) {
		    return new NotificationContent(in);
		}

		public NotificationContent[] newArray(int size) {
		    return new NotificationContent[size];
		}
	};
}
