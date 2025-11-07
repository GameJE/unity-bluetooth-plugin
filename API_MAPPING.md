# BLE Plugin API Mapping

기존 BluetoothLEHardwareInterface API와 새 UnityBLEPlugin Android 구현 매핑 문서

## 필수 구현 API (Central Mode)

| # | 기존 API | 메시지 형식 | 새 Android 구현 | 구현 상태 |
|---|----------|-------------|----------------|----------|
| 1 | `Initialize(bool, bool, Action, Action<string>)` | `Initialized` | `UnityBLEPlugin.Initialize()` | ✅ |
| 2 | `DeInitialize(Action)` | `DeInitialized` | `UnityBLEPlugin.DeInitialize()` | ✅ |
| 3 | `ScanForPeripheralsWithServices(...)` | `DiscoveredPeripheral~address~name~rssi~base64data` | `BLEScanManager.ScanForPeripheralsWithServices()` | ✅ |
| 4 | `StopScan()` | - | `BLEScanManager.StopScan()` | ✅ |
| 5 | `ConnectToPeripheral(...)` | `ConnectedPeripheral~address` | `BLEConnectionManager.ConnectToPeripheral()` | ✅ |
| 6 | `DisconnectPeripheral(string, Action)` | `DisconnectedPeripheral~address` | `BLEConnectionManager.DisconnectPeripheral()` | ✅ |
| 7 | `DisconnectAll()` | - | `BLEConnectionManager.DisconnectAll()` | ✅ |
| 8 | `SubscribeCharacteristic(...)` | `DidUpdateNotificationStateForCharacteristic~char~address`<br>`DidUpdateValueForCharacteristic~char~address~base64data` | `BLEGattManager.SubscribeCharacteristic()` | ✅ |
| 9 | `UnSubscribeCharacteristic(...)` | - | `BLEGattManager.UnSubscribeCharacteristic()` | ✅ |
| 10 | `ReadCharacteristic(...)` | `DidUpdateValueForCharacteristic~char~address~base64data` | `BLEGattManager.ReadCharacteristic()` | ✅ |
| 11 | `WriteCharacteristic(...)` | `DidWriteCharacteristic~char` | `BLEGattManager.WriteCharacteristic()` | ✅ |
| 12 | `RequestMtu(string, int, Action)` | `MtuChanged~address~mtu` | `BLEGattManager.RequestMtu()` | ✅ |
| 13 | `BluetoothConnectionPriority(string, ConnectionPriority)` | - | `UnityBLEPlugin.SetConnectionPriority()` | ✅ |

## 서비스/Characteristic 발견 메시지

| 메시지 타입 | 형식 | Android 구현 |
|------------|------|--------------|
| Service 발견 | `DiscoveredService~address~serviceUUID` | BLEConnectionManager.onServicesDiscovered() |
| Characteristic 발견 | `DiscoveredCharacteristic~address~serviceUUID~charUUID` | BLEConnectionManager.onServicesDiscovered() |

## 에러 처리

| 메시지 타입 | 형식 | Android 구현 |
|------------|------|--------------|
| 에러 | `Error~errorMessage` | UnityBLEBridge.sendError() |

## 미구현 API (현재 프로젝트 미사용)

| API | 용도 | 비고 |
|-----|------|------|
| `BluetoothEnable(bool)` | 블루투스 활성화 | Android 전용, 다이얼로그 표시만 |
| `ReadRSSI(string, Action)` | 신호 강도 읽기 | 향후 추가 가능 |
| `ScanForBeacons(...)` | iBeacon 스캔 | Peripheral 모드, 미사용 |
| `CreateService(...)` | Peripheral 모드 | 미사용 |
| `CreateCharacteristic(...)` | Peripheral 모드 | 미사용 |
| `StartAdvertising(...)` | Peripheral 모드 | 미사용 |
| `StopAdvertising(...)` | Peripheral 모드 | 미사용 |

## 메시지 프로토콜 차이점

### 기존 플러그인 (문자열 구분자 방식)
```
"DiscoveredPeripheral~AA:BB:CC:DD:EE:FF~Device Name~-65~base64encodeddata"
```
- 구분자: `~` (Tilde)
- Base64 인코딩된 광고 데이터

### 새 플러그인 (JSON 방식)
```json
{
  "type": "OnDeviceDiscovered",
  "address": "AA:BB:CC:DD:EE:FF",
  "name": "Device Name",
  "rssi": -65,
  "data": "base64encodeddata"
}
```
- JSON 직렬화
- 명확한 타입 식별
- 확장성 우수

## Unity C# 구현 계획

### 1. BluetoothDeviceScript 수정
- `OnBluetoothMessage(string message)` 메서드를 JSON 파싱으로 변경
- 기존 `~` 구분자 방식과 JSON 방식 모두 지원 (하위 호환성)

### 2. BluetoothLEHardwareInterface 수정
- Android 네이티브 호출을 `UnityBLEPlugin` 클래스로 변경
- 기존 API 시그니처 유지 (100% 호환성)

## 변경 필요 사항

### Android Native → Unity C# 메시지 변환

| Android BLEMessage 타입 | Unity 메시지 문자열 | BluetoothDeviceScript Action |
|------------------------|---------------------|------------------------------|
| `TYPE_INITIALIZED` | `Initialized` | `InitializedAction()` |
| `TYPE_ERROR` | `Error~{error}` | `ErrorAction(error)` |
| `TYPE_DEVICE_DISCOVERED` | `DiscoveredPeripheral~{address}~{name}~{rssi}~{data}` | `DiscoveredPeripheralAction(address, name)`<br>`DiscoveredPeripheralWithAdvertisingInfoAction(address, name, rssi, data)` |
| `TYPE_DEVICE_CONNECTED` | `ConnectedPeripheral~{address}` | `ConnectedPeripheralAction(address)` |
| `TYPE_DEVICE_DISCONNECTED` | `DisconnectedPeripheral~{address}` | `DisconnectedPeripheralAction(address)` |
| `TYPE_SERVICE_DISCOVERED` | `DiscoveredService~{address}~{serviceUUID}` | `DiscoveredServiceAction(address, serviceUUID)` |
| `TYPE_CHARACTERISTIC_DISCOVERED` | `DiscoveredCharacteristic~{address}~{serviceUUID}~{charUUID}` | `DiscoveredCharacteristicAction(address, serviceUUID, charUUID)` |
| `TYPE_DATA_RECEIVED` | `DidUpdateValueForCharacteristic~{charUUID}~{address}~{base64data}` | `DidUpdateCharacteristicValueAction[charUUID][action](charUUID, data)` |
| `TYPE_MTU_CHANGED` | `MtuChanged~{address}~{mtu}` | `RequestMtuAction(address, mtu)` |

## 구현 우선순위

### Phase 1: 핵심 API (Dolittle 프로젝트 필수)
1. ✅ Initialize / DeInitialize
2. ✅ ScanForPeripheralsWithServices / StopScan
3. ✅ ConnectToPeripheral / DisconnectPeripheral
4. ✅ SubscribeCharacteristic / UnSubscribeCharacteristic
5. ✅ ReadCharacteristic / WriteCharacteristic
6. ✅ RequestMtu

### Phase 2: 메시지 변환 레이어
7. ⏳ BLEMessage → 기존 메시지 형식 변환 (UnityBLEBridge 수정 필요)
8. ⏳ BluetoothDeviceScript JSON 파싱 추가

### Phase 3: 통합 테스트
9. ⏳ BLEScanService 통합 테스트
10. ⏳ BLEConnectService 통합 테스트
11. ⏳ DolittleService 계열 통합 테스트

## 작성 정보

- **작성일**: 2025-11-06
- **버전**: 1.0
- **상태**: Android 네이티브 구현 완료, Unity C# 통합 대기
