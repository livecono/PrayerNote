# Firebase 백업 기능 구현 가이드

## 개요
이 기능은 사용자가 선택한 대상자(Person)와 기도제목(PrayerTopic)을 Firebase Firestore에 백업할 수 있게 합니다.

## 구현된 기능

### 1. Firebase Backup Service (`FirebaseBackupService`)
- Firebase Authentication과 Firestore를 사용하여 데이터를 백업합니다
- 사용자별로 백업 데이터를 관리합니다
- 각 백업 세션에 대상자와 기도제목을 저장합니다

**주요 메서드:**
- `backupSelectedData()` - 선택된 대상자와 기도제목 백업
- `getBackupSessions()` - 백업 세션 목록 조회
- `getBackupPersons()` - 특정 세션의 대상자 조회
- `getBackupTopics()` - 특정 세션의 기도제목 조회
- `deleteBackupSession()` - 백업 세션 삭제

### 2. 데이터 모델 (`FirebaseBackupModel`)
```kotlin
// 대상자 백업 데이터
data class PersonBackup(
    val id: String,              // Firebase Document ID
    val originalId: Long,        // 원본 ID
    val name: String,
    val memo: String,
    val colorTag: String,
    val createdAt: Date?,
    val updatedAt: Date?,
    val backupTimestamp: Date?   // 서버 타임스탐프
)

// 기도제목 백업 데이터
data class PrayerTopicBackup(
    val id: String,
    val originalId: Long,
    val personId: Long,
    val personBackupId: String,
    val title: String,
    val priority: Int,
    val status: String,
    val createdAt: Date?,
    val answeredAt: Date?,
    val backupTimestamp: Date?
)

// 백업 세션 정보
data class BackupSession(
    val id: String,
    val userId: String,
    val personBackupIds: List<String>,
    val topicBackupIds: List<String>,
    val totalPersons: Int,
    val totalTopics: Int,
    val timestamp: Date?,
    val status: String
)
```

### 3. Firebase Firestore 구조
```
backups/
├── {userId}/
    └── sessions/
        └── {backupId}/
            ├── [session document]
            ├── persons/
            │   └── {personDocId} -> PersonBackup
            └── topics/
                └── {topicDocId} -> PrayerTopicBackup
```

### 4. UI 컴포넌트 (`FirebaseBackupScreen`)
- 대상자 선택 섹션
- 기도제목 선택 섹션
- 모두 선택/선택 해제 기능
- 백업 진행 상황 표시
- 선택된 항목 개수 표시

### 5. SettingsViewModel 업데이트
새로운 상태와 메서드:
- `backupInProgress` - 백업 진행 중 여부
- `selectedPersons` - 선택된 대상자 목록
- `selectedTopics` - 선택된 기도제목 목록
- `allPersons` - 전체 대상자 목록
- `allTopics` - 전체 기도제목 목록

**새로운 메서드:**
- `togglePersonSelection()` - 대상자 선택 토글
- `toggleTopicSelection()` - 기도제목 선택 토글
- `selectAllPersons()` - 모든 대상자 선택
- `selectAllTopics()` - 모든 기도제목 선택
- `clearPersonSelection()` - 대상자 선택 해제
- `clearTopicSelection()` - 기도제목 선택 해제
- `backupToFirebase()` - Firebase 백업 실행

### 6. 의존성 주입 설정
- `FirebaseModule` - Firebase Auth, Firestore 제공
- `BackupModule` - BackupRepository 제공

## 사용 방법

### 1. Settings 화면에서 Firebase 백업
1. Settings 화면으로 이동
2. "Backup Section"에서 "Firebase 백업" 버튼 클릭
3. 대상자와 기도제목 선택
4. "백업" 버튼 클릭

### 2. 대상자 선택
- 체크박스를 클릭하여 개별 선택
- "모두 선택" 버튼으로 전체 선택
- "선택 해제" 버튼으로 모두 해제

### 3. 기도제목 선택
- 대상자별로 기도제목 확인 가능
- 같은 방식으로 개별/전체 선택 가능

## Firebase 설정 필수 사항

### 1. Firebase 프로젝트 생성
- Firebase Console에서 프로젝트 생성
- Android 앱 추가
- `google-services.json` 다운로드 및 `app/` 디렉토리에 저장

### 2. Firebase Authentication 활성화
- Authentication > Sign-in method
- Anonymous 또는 원하는 인증 방식 활성화

### 3. Firestore 데이터베이스 생성
- Firestore > 데이터베이스 만들기
- 보안 규칙 설정:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // 사용자의 백업 데이터만 접근 가능
    match /backups/{userId}/{document=**} {
      allow read, write: if request.auth.uid == userId;
    }
  }
}
```

## 보안 고려사항

1. **인증**: Firebase Authentication으로 사용자 검증
2. **Firestore 보안 규칙**: 사용자별 데이터 격리
3. **데이터 암호화**: Firestore의 암호화 전송
4. **접근 제어**: 자신의 백업 데이터만 접근 가능

## 향후 확장 기능

1. **복원 기능**
   - 백업된 데이터를 로컬 데이터베이스로 복원
   - 병합 또는 덮어쓰기 옵션

2. **백업 관리**
   - 백업 목록 조회
   - 선택적 삭제
   - 타임스탬프 표시

3. **자동 백업**
   - 주기적 백업 스케줄
   - Wi-Fi 연결 시에만 백업

4. **동기화**
   - 여러 기기 간 자동 동기화
   - 실시간 업데이트

## 파일 구조

```
app/src/main/kotlin/com/prayernote/app/
├── data/
│   ├── firebase/
│   │   ├── model/
│   │   │   └── FirebaseBackupModel.kt
│   │   └── service/
│   │       └── FirebaseBackupService.kt
│   ├── repository/
│   │   └── BackupRepositoryImpl.kt
├── domain/
│   └── repository/
│       └── BackupRepository.kt
├── di/
│   ├── FirebaseModule.kt
│   └── BackupModule.kt
└── presentation/
    ├── screen/
    │   ├── FirebaseBackupScreen.kt
    │   └── SettingsScreen.kt
    └── viewmodel/
        └── SettingsViewModel.kt
```

## 테스트 방법

1. Firebase Console에서 Firestore 데이터 확인
2. Settings > Firebase 백업 버튼 클릭
3. 대상자와 기도제목 선택
4. 백업 버튼 클릭
5. 성공 메시지 확인
6. Firebase Console에서 백업된 데이터 검증

## 트러블슈팅

### 로그인 오류
```
사용자가 로그인하지 않았습니다
```
→ Firebase Authentication 설정 확인, Anonymous 로그인 활성화

### Firestore 접근 오류
```
Permission denied
```
→ Firestore 보안 규칙 확인

### 데이터 저장 실패
```
Failed to backup data
```
→ 네트워크 연결 확인, Firebase 프로젝트 설정 확인
