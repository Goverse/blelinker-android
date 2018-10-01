
## blelinker-android

[![API](https://img.shields.io/badge/API-19%2B-brightgreen.svg)](https://android-arsenal.com/api?level=19)
[![License](https://img.shields.io/badge/license-Apache%202-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)

## 介绍
基于安卓蓝牙相关接口的封装库，主要功能有：扫描，连接，开启蓝牙服务等。

## 如何使用

### 在Gradle中添加依赖
```java
implementation 'me.goverse.blelinker:blelinker:1.0.0'
```
### 设备扫描

```
BLEScanner mBLEScanner = newBLEScanner(context);
mBLEScanner.startLeScan(new BLEScanner.OnScanedListener() {
                @Override
                public void onScaned(BtDevice bleDevice) {
                }
            });
```
### 设备连接
#### 1. 初始化
```
BLEClient.getInstance().init(mContext);    
```
#### 2. 配置Channels
```
//读取Channel,对应GATT读操作
ReadChannel readChannel = BLEClient.getInstance().addReadChannel(serviceUUID,characteristicUUID);

//写入Channel,对应GATT写操作
WriteChannel writeChannel = BLEClient.getInstance().addWriteChannel(serviceUUID, characteristicUUID);

//通知Channel,对应GATT通知操作
BLEClient.getInstance().addNotificationChannel(serviceUUID, characteristicUUID, descriptorUUID, mOnNotificationListener);
```
#### 3. 读,写，通知
```
readChannel.read(onReadListener);

writeChannel.write(data, onWriteListener);

//通知回调接口
public interface OnNotificationListener {
        public void onNotificationRecieved(byte[] data);
    }
```
### GATT服务
```
BLEServer bleServer = new BLEServer(context);
bleServer.openServer();
AncsService mAncsService = new AncsService(UUIDs.ANCSUUIDs.SERVICE, UUIDs.ANCSUUIDs.NOTIFICATION_SOURCE, UUIDs.ANCSUUIDs.CONTROL_POINT, UUIDs.ANCSUUIDs.DATA_SOURCE, UUIDs.DescriptorUUIDs.CCCD);
bleServer.addService(mAncsService);
```
