# Changelog

## [1.1.0] - 2025-11-06

### Added
- **연결 우선순위 설정 기능 추가**
  - `BLEConnectionManager.SetConnectionPriority(String address, int priority)` 메서드 추가
  - `UnityBLEPlugin.SetConnectionPriority(String nameOrAddress, int priority)` 공개 API 추가
  - Android BLE CONNECTION_PRIORITY_HIGH/BALANCED/LOW_POWER 지원

### Changed
- Unity C# API 시그니처 변경
  - `BluetoothConnectionPriority(ConnectionPriority)` → `BluetoothConnectionPriority(string nameOrAddress, ConnectionPriority)`
  - 디바이스별 연결 우선순위 개별 설정 가능

### Technical Details
- `BluetoothGatt.requestConnectionPriority()` 네이티브 API 사용
- 메인 스레드에서 안전하게 실행 (ThreadHelper 사용)
- 디바이스 이름 또는 주소로 호출 가능 (자동 해석)

### Usage Example
```csharp
// Unity C#에서 호출
BluetoothLEHardwareInterface.BluetoothConnectionPriority(
    device.Address,
    BluetoothLEHardwareInterface.ConnectionPriority.High
);
```

### Priority Values (Android BluetoothGatt Constants)
- `CONNECTION_PRIORITY_BALANCED (0)`: 균형 모드 (기본값, 30-50ms interval)
- `CONNECTION_PRIORITY_HIGH (1)`: 최소 지연, 최대 대역폭 (심음 스트리밍 등에 적합, 7.5-15ms interval)
- `CONNECTION_PRIORITY_LOW_POWER (2)`: 저전력 모드 (100-125ms interval)

### Important Notes
- **현재 연결 우선순위 읽기 불가**: Android BluetoothGatt API는 현재 우선순위를 조회하는 메서드를 제공하지 않음
- **타이밍**: 서비스 발견 완료 후 500ms 지연을 두고 호출하여 안정성 확보
- **성공 여부**: `requestConnectionPriority()` 반환값으로만 성공/실패 확인 가능

---

## [1.0.0] - 2025-11-06

### Initial Release
- BLE Central 모드 기본 기능 구현
- 디바이스 스캔, 연결, 서비스/특성 탐색
- 특성 읽기/쓰기/구독
- MTU 요청
