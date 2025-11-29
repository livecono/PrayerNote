# PrayerNote (기도노트)

기도 대상자와 기도제목을 관리하는 안드로이드 애플리케이션입니다.

## 주요 기능

- 기도 대상자 관리
- 각 대상자의 기도제목 관리
- 요일별 기도 대상자 할당
- 응답된 기도 완료 리스트
- 매일 설정한 시간에 해당 요일의 기도대상자와 기도제목 알림
- 기도 통계 및 히스토리 (차트)
- 로컬 백업/복원 (JSON, CSV)
- 홈 스크린 위젯

## 기술 스택

- **언어**: Kotlin
- **UI**: Jetpack Compose, Material Design 3
- **아키텍처**: MVVM
- **데이터베이스**: Room
- **의존성 주입**: Hilt
- **비동기**: Coroutines, Flow
- **알림**: WorkManager, NotificationManager
- **위젯**: Glance
- **차트**: MPAndroidChart
- **백업**: Gson (JSON), OpenCSV (CSV), Apache Commons Compress (ZIP)
- **기타**: Navigation Compose, Paging3, DataStore, Reorderable List

## 프로젝트 구조

```
app/
├── data/
│   ├── local/
│   │   ├── entity/        # Room 엔티티
│   │   ├── dao/           # DAO 인터페이스
│   │   └── PrayerDatabase.kt
│   └── repository/        # Repository 구현
├── domain/
│   └── repository/        # Repository 인터페이스
├── presentation/
│   ├── screen/            # Compose 화면
│   ├── viewmodel/         # ViewModel
│   └── navigation/        # Navigation
├── worker/                # WorkManager Workers
├── widget/                # Glance Widgets
├── util/                  # 유틸리티 (BackupManager 등)
└── ui/
    └── theme/             # Material Design 3 테마
```

## 요구사항

- Minimum SDK: 24 (Android 7.0)
- Target SDK: 34 (Android 14)
- Kotlin 1.9.20+
- Gradle 8.2.0+

## 빌드 방법

1. Android Studio에서 프로젝트 열기
2. Gradle 동기화 대기
3. 빌드 및 실행

```bash
./gradlew assembleDebug
```

## 라이선스

이 프로젝트는 개인 프로젝트입니다.
