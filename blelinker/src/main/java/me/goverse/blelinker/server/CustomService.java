package me.goverse.blelinker.server;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import static me.goverse.blelinker.server.ancs.util.ValueUtils.spiltToMTUs;

/**
 * @author gaoyu
 */

public abstract class CustomService extends BluetoothGattService implements BLEServer.IServerListener{

    /**
     * ResponseData
     */
    public class ResponseData {

        private byte[] value;

        private UUID charUuid;

        public byte[] getValue() {
            return value;
        }

        public ResponseData(UUID charUuid, byte[] value) {
            this.charUuid = charUuid;
            this.value = value;
        }

        public void setValue(byte[] value) {
            this.value = value;
        }

        public UUID getCharUuid() {
            return charUuid;
        }

        public void setCharUuid(UUID charUuid) {
            this.charUuid = charUuid;
        }
    }

    private BLEServer mBleServer = null;

    protected static final String TAG = "bleService";

    /**
     * NotifyDeviceMap
     */
    protected Map<BluetoothDevice, Set<UUID>> mNotifyDeviceMap = new HashMap<>();

    public CustomService(UUID uuid) {
        super(uuid, BluetoothGattService.SERVICE_TYPE_PRIMARY);
    }

    /**
     * addCharacteristicToDevice
     * @param device device
     * @param characteristicUuid characteristicUuid
     */
    protected void addCharacteristicToDevice(BluetoothDevice device,
                                           UUID characteristicUuid) {
        Log.d(TAG, "addCharacteristicToDevice");
        Set<UUID> characteristicUuidSet = mNotifyDeviceMap.get(device);
        if (characteristicUuidSet == null) {
            characteristicUuidSet = new TreeSet<UUID>();
            mNotifyDeviceMap.put(device, characteristicUuidSet);
        }
        characteristicUuidSet.add(characteristicUuid);
    }

    /**
     * removeCharacteristicFromDevice
     * @param device device
     * @param uuid uuid
     */
    protected void removeCharacteristicFromDevice(BluetoothDevice device,
                                                UUID uuid) {
        Log.d(TAG, "removeCharacteristicFromDevice---" + "device:" + device.getAddress() + "uuid:" + uuid);
        Set<UUID> characteristicUuidSet = mNotifyDeviceMap.get(device);
        if (characteristicUuidSet != null) {
            characteristicUuidSet.remove(uuid);
        }
    }

    /**
     * removeNotifyDevice
     * @param device device
     */
    protected void removeNotifyDevice(BluetoothDevice device) {
        Log.d(TAG, "removeNotifyDevice---" + "device:" + device.getAddress());
        mNotifyDeviceMap.remove(device);
    }

    /**
     * setBleServer
     * @param bleServer bleServer
     */
    public void setBleServer(BLEServer bleServer) {
        Log.d(TAG, "setBleServer---" + "bleServer:" + bleServer.getClass().getSimpleName());
        mBleServer  = bleServer;
    }

    @Override
    public void onConnected(BluetoothDevice device) {
        Log.d(TAG, "onConnected:" + device.getAddress());

    }

    @Override
    public void onDisConnected(BluetoothDevice device) {
        Log.d(TAG, "onDisConnected:" + device.getAddress());
    }

    @Override
    public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
        Log.d(TAG, "onCharacteristicReadRequest:" + device.getAddress() + "---charUUID:" + characteristic.getUuid());
        sendCharResSuccess(device, requestId, offset, characteristic);
    }

    @Override
    public void onCharacteristicWriteRequest(BluetoothDevice device,
                                             int requestId,
                                             BluetoothGattCharacteristic characteristic,
                                             boolean preparedWrite,
                                             boolean responseNeeded,
                                             int offset,
                                             byte[] value) {
        Log.d(TAG, "onCharacteristicWriteRequest:" + device.getAddress() + "---charUUID:" + characteristic.getUuid());
        characteristic.setValue(value);
        sendCharResSuccess(device, requestId, offset, characteristic);

        ResponseData reponseData = fetchResponseData(value, characteristic.getUuid());
        if (null == reponseData || null == reponseData.getValue()) {
            return;
        }
        List<byte[]> mtus = spiltToMTUs(reponseData.getValue(), getMtuSize());
        for (int i = 0; i < mtus.size(); i ++) {
            writeCharacteristicChanged(device, reponseData.getCharUuid(), mtus.get(i));
        }
        onResponseNotified();
    }

    @Override
    public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
        Log.d(TAG, "onDescriptorReadRequest:" + device.getAddress() + "---charUUID:" + descriptor.getUuid());
        sendDesResSuccess(device, requestId, offset, descriptor);
    }

    @Override
    public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
        Log.d(TAG, "onDescriptorWriteRequest:" + device.getAddress() + "---charUUID:" + descriptor.getUuid());
        descriptor.setValue(value);
        if (Arrays.equals(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE, value) || Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, value)) {

            addCharacteristicToDevice(device, descriptor.getCharacteristic().getUuid());
        } else if (Arrays.equals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE, value)) {

            removeCharacteristicFromDevice(device, descriptor.getCharacteristic().getUuid());
        }
        sendDesResSuccess(device, requestId, offset, descriptor);
    }

    /**
     * fetchResponseData
     * @param requestValue requestValue
     * @param charUuid charUuid
     * @return byte[]
     */
    protected abstract ResponseData fetchResponseData(byte[] requestValue, UUID charUuid);

    /**
     * onResponseNotified
     */
    protected abstract void onResponseNotified();

    /**
     * MTU大小，默认不分割
     * @return int
     */
    public int getMtuSize() {
        return Integer.MAX_VALUE;
    }

    /**
     * writeCharacteristicChanged
     * @param device device
     * @param uuid uuid
     * @param value value
     */
    protected synchronized void writeCharacteristicChanged(BluetoothDevice device, UUID uuid, byte[] value) {
        Log.d(TAG, "writeCharacteristicChanged:" + device.getAddress() + "---charUUID:" + uuid);
        getCharacteristic(uuid).setValue(value);
        mBleServer.notifyCharacteristicChanged(device, getCharacteristic(uuid), false);
    }

    /**
     * addCharacteristic
     * @param uuid charUuid
     * @param properties properties
     * @param permissions permissions
     * @param descriptorUuid descriptorUuid
     * @param descriptorValue descriptorValue
     */
    protected void addCharacteristic(UUID uuid, int properties, int permissions, UUID descriptorUuid, byte[] descriptorValue) {

        BluetoothGattCharacteristic ch =
                new BluetoothGattCharacteristic(
                        uuid,
                        properties,
                        permissions);
        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(
                descriptorUuid,
                BluetoothGattDescriptor.PERMISSION_WRITE
                        | BluetoothGattDescriptor.PERMISSION_READ);
        descriptor.setValue(descriptorValue);
        ch.addDescriptor(descriptor);
        addCharacteristic(ch);
    }

    private void sendCharResSuccess(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
        mBleServer.sendResponse(device, requestId,
                BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
    }

    private void sendDesResSuccess(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
        mBleServer.sendResponse(device, requestId,
                BluetoothGatt.GATT_SUCCESS, offset, descriptor.getValue());
    }

}
