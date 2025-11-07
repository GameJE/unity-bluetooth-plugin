# Unity classes.jar 배치 안내

이 디렉토리에 Unity의 `classes.jar` 파일을 배치해야 합니다.

## classes.jar 위치

Unity 설치 경로에서 다음 위치에 있습니다:

```
C:/Program Files/Unity/Hub/Editor/[버전]/Editor/Data/PlaybackEngines/AndroidPlayer/Variations/mono/Release/Classes/classes.jar
```

또는

```
/Applications/Unity/Hub/Editor/[버전]/PlaybackEngines/AndroidPlayer/Variations/mono/Release/Classes/classes.jar
```

## 복사 방법

1. Unity 설치 경로에서 `classes.jar` 파일을 찾습니다
2. 이 디렉토리(`app/libs/`)에 복사합니다
3. Gradle Sync를 실행합니다

## 주의사항

- `classes.jar`는 Unity와의 통신을 위해 필요한 `UnityPlayer` 클래스를 포함합니다
- 이 파일은 `compileOnly`로 설정되어 있어 AAR 빌드 시 포함되지 않습니다
- Unity 프로젝트에 통합 시 Unity 자체의 `classes.jar`를 사용합니다
