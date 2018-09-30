package me.goverse.blelinker.server.ancs;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import me.goverse.blelinker.server.CustomService;
import me.goverse.blelinker.server.ancs.constant.AncsID;
import me.goverse.blelinker.server.ancs.notification.Notification;
import me.goverse.blelinker.server.ancs.notification.NotificationCache;
import me.goverse.blelinker.server.ancs.notification.NotificationContent;
import me.goverse.blelinker.server.ancs.util.ValueUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static me.goverse.blelinker.server.ancs.util.ValueUtils.putString;

/**
 * @author gaoyu
 */

public class AncsService extends CustomService {

    private static final int MTU_SIZE = 18;

    protected UUID mControlPoint = null;

    protected UUID mDataSource = null;

    protected UUID mNotificationSource = null;

    private boolean mIsSending = false;

    private byte[] mSendingAcquireLock = new byte[0];

    private HandlerThread mSendThread = null;

    private Handler mHandler = null;

    private NotificationCache mNotificationCache = new NotificationCache();

    // 事项提醒之后添加5个空格
    private boolean isAdd5Blank = false;

    public AncsService(UUID uuid, UUID notificationSource, UUID controlPoint, UUID dataSource, UUID clientCharConfig) {

        super(uuid);
        mDataSource = dataSource;
        mControlPoint = controlPoint;
        mNotificationSource = notificationSource;
        setUpService(mNotificationSource, mControlPoint, mDataSource, clientCharConfig);
        mSendThread = new HandlerThread("Sending");
        mSendThread.start();
        mHandler = new Handler(mSendThread.getLooper());
    }

    private void setUpService(UUID notificationSource,
                              UUID controlPoint,
                              UUID dataSource,
                              UUID clientCharConfig) {
        addCharacteristic(notificationSource,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ,
                clientCharConfig,
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        addCharacteristic(controlPoint,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE,
                clientCharConfig,
                new byte[]{0x00, 0x00});
        addCharacteristic(dataSource,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ,
                clientCharConfig,
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
    }


    /**
     * addNotification
     * @param notification notification
     */
    public void addNotification(Notification notification) {

        cacheNotification(notification);
        sendNotification();
    }


    /**
     * removeNotification
     * @param notificationId notificationId
     */
    public void removeNotification(int notificationId) {

    }

    private void cleanExpiredNotification(Notification notification) {

        if (checkIsPhoneCall(notification.getContent().getCategoryId())) {
            mNotificationCache.removeExpiredIdle(notification.getAddedTime()-1);
        }
        mNotificationCache.removeExpiredAll(notification.getAddedTime() - 30 * 1000);
        mNotificationCache.removeExpiredIdle(notification.getAddedTime() - 3000);

    }

    private void cacheNotification(Notification notification) {
        mNotificationCache.addNotification(notification);
        cleanExpiredNotification(notification);
    }

    private boolean checkIsPhoneCall(byte categoryId) {
        return AncsID.Category.INCOMING_CALL == categoryId
                || AncsID.Category.MISSED_CALL == categoryId;
    }

    @Override
    public void onConnected(BluetoothDevice device) {
        super.onConnected(device);
        addCharacteristicToDevice(device, mNotificationSource);
    }

    @Override
    public void onDisConnected(BluetoothDevice device) {
        super.onDisConnected(device);
        removeNotifyDevice(device);
    }

    @Override
    protected ResponseData fetchResponseData(byte[] requestValue, UUID uuid) {
        Log.d(TAG, "fetchResponseData");
        if (!checkIsAncsRequest(requestValue) || !(uuid.toString().equals(mControlPoint.toString()))) {
            return null;
        }

        AncsCommand ancsCommand = new AncsCommand(requestValue);
        int notificationId = ancsCommand.getNotificationUID();
        if (!checkHasRequestNotification(notificationId)) {
            releaseSendingLock();
            sendNotification();
            return null;
        }

        removeNotificationBefore(notificationId);
        byte[] data = buildDataSource(ancsCommand, getCachedNotification(notificationId));
        return new ResponseData(mDataSource, data);
    }

    @Override
    protected void onResponseNotified() {
        Log.d(TAG, "onResponseNotified");
        releaseSendingLock();
        sendNotification();
    }

    private void acquireSendingLock() {
        synchronized (mSendingAcquireLock) {
            while (mIsSending) {
                try {
                    mSendingAcquireLock.wait(1000);
                    break;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            mIsSending = true;
        }
    }

    private void releaseSendingLock() {
        synchronized (mSendingAcquireLock) {
            mIsSending = false;
            mSendingAcquireLock.notify();
        }
    }

    private void sendNotification() {
        Log.d(TAG, "sendNotification");
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                acquireSendingLock();
                Notification notification = mNotificationCache.getFirstUnsentNotification();
                if (notification == null) {
                    return;
                }

                notification.setSending(true);
                byte[] responseValue = ValueUtils.obtainNotificationSource(
                        notification.getEventId(),
                        AncsID.AncsEventFlag.SILENT,
                        notification.getContent().getCategoryId(),
                        (byte) 1,
                        ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
                                .putInt(notification.getId()).array());

                notifyAllDevices(responseValue);
            }
        });
    }

    private void notifyAllDevices(byte[] responseValue) {
        for (Map.Entry<BluetoothDevice, Set<UUID>> entry : mNotifyDeviceMap.entrySet()) {
            if (entry.getValue().contains(mNotificationSource)) {
                writeCharacteristicChanged(entry.getKey(), mNotificationSource, responseValue);
            }
        }
    }

    private boolean checkHasRequestNotification(int notificationId) {
        return null != getCachedNotification(notificationId);
    }

    private boolean checkIsAncsRequest(byte[] requestValue) {
        return requestValue != null && requestValue.length > 8;
    }

    private Notification getCachedNotification(int notificationId) {
        return mNotificationCache.getLatestSentNotification(notificationId);
    }

    private void removeNotificationBefore(int notificationId) {
        mNotificationCache.removeAllBefore(notificationId);
    }

    private byte[] buildDataSource(AncsCommand ancsCommand, Notification notification) {

        NotificationContent content = notification.getContent();
        ByteBuffer bb = ByteBuffer.allocate(ancsCommand.getResponseLen());
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.put(ancsCommand.getValue(), 0, 5);
        for (int i = 0; i < ancsCommand.getAttributes().size(); i++) {
            Map.Entry<Byte, Short> entry = ancsCommand.getAttributes().get(i);
            switch (entry.getKey()) {
                case AncsID.NotificationAttribute.TITLE:
                    putStringEntry(entry, bb, content.getTitle());
                    break;
                case AncsID.NotificationAttribute.MESSAGE:
                    if (content.getMessage().contains("提醒") || content.getMessage().contains("Reminder") && isAdd5Blank) {
                        content.setMessage(content.getMessage() + "     ");
                    }

                    String message = content.getMessage();
                    if ("com.twitter.android".equals(content.getPackageName()) || "com.skype.raider".equals(content.getPackageName())) {
                        String[] array = message.split("\\n");
                        if (array != null && array.length > 0) {
                            message = array[array.length / 2];
                        }
                    }
                    putStringEntry(entry, bb, message);
                    break;
                case AncsID.NotificationAttribute.APP_IDENTIFIER:
                    String packageName = content.getPackageName();
                    packageName = packageName.replace("com.yf.sms.dummy", "com.apple.MobileSMS");
                    packageName = packageName.replace("com.facebook.katana", "com.facebook.Facebook");
                    packageName = packageName.replace("com.twitter.android", "com.atebits.Tweetie2");
                    packageName = packageName.replace("com.whatsapp", "net.whatsapp.WhatsApp");
                    packageName = packageName.replace("com.skype.raider", "com.skype.skype");
                    packageName = packageName.replace("com.tencent.mobileqq", "com.tencent.mqq");
                    packageName = packageName.replace("com.tencent.mm", "com.tencent.xin");
                    packageName = packageName.replace("com.immomo.momo", "com.wemomo.momoappdemo1");
                    // 如果是我们的app并且包含提醒两个字，则后面追加5个空格，用于固件7次震动
                    if (packageName.contains("com.yf.smart.weloopx.alpha") || packageName.contains("com.yf.smart.weloopx.beta")
                            || packageName.contains("com.yf.smart.weloopx.dist")) {
                        isAdd5Blank = true;
                    } else {
                        isAdd5Blank = false;
                    }
                    putStringEntry(entry, bb, packageName);
                    setMessageSize(bb, entry.getKey(), (short) content.getMessage().getBytes(AncsID.CHARSET).length);
                    break;
                case AncsID.NotificationAttribute.MESSAGE_SIZE:
                    setMessageSize(bb, entry.getKey(), (short) content.getMessage().getBytes(AncsID.CHARSET).length);
                    break;
                default:
                    break;
            }
        }

        byte[] responseValue = new byte[bb.position()];
        bb.rewind();
        bb.get(responseValue);
        return responseValue;
    }

    private void setMessageSize(ByteBuffer bb, Byte attribute, short size) {

        bb.put(attribute);
        bb.put((byte) 0x02);
        bb.put((byte) 0x00);
        bb.putShort(size);
    }

    private void putStringEntry(Map.Entry<Byte, Short> entry, ByteBuffer bb,
                                String string) {
        bb.put(entry.getKey());
        putString(bb, string, entry.getValue());
    }


    @Override
    public int getMtuSize() {
        return MTU_SIZE;
    }

}
