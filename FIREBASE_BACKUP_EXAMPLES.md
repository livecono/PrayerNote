# Firebase 백업 기능 - 사용 예제

## 화면 흐름

### 1. Settings 화면 → Firebase 백업 버튼 클릭

```kotlin
// SettingsScreen.kt에서 버튼 클릭 시
Button(
    onClick = { showFirebaseBackupDialog = true },
    colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.tertiary
    )
) {
    Text("Firebase 백업")
}
```

### 2. Firebase 백업 다이얼로그 표시

```
┌─────────────────────────────────────┐
│        Firebase 백업                  │
├─────────────────────────────────────┤
│                                      │
│ 대상자 선택                           │
│ 선택됨: 0 / 전체: 5                  │
│ ┌─────────────┬────────────┐       │
│ │ 모두 선택   │ 선택 해제  │       │
│ └─────────────┴────────────┘       │
│ ─────────────────────────────────  │
│ ☐ 홍길동                           │
│ ☐ 김영희                           │
│ ☐ 박민수                           │
│ ☐ 이순신                           │
│ ☐ 장보고                           │
│                                    │
│ 기도제목 선택                        │
│ 선택됨: 0 / 전체: 12               │
│ ┌─────────────┬────────────┐      │
│ │ 모두 선택   │ 선택 해제  │      │
│ └─────────────┴────────────┘      │
│ ───────────────────────────────── │
│ ☐ 가정의 평안                      │
│ ☐ 사역의 축복                      │
│ ☐ 건강한 삶                        │
│ ... (더 많은 항목)                  │
│                                    │
│ 0명의 대상자와 0개의 기도제목이     │
│ 백업됩니다.                         │
│                                    │
├─────────────────────────────────────┤
│ [백업]                  [취소]       │
└─────────────────────────────────────┘
```

### 3. 대상자 선택 예제

사용자가 다음과 같이 선택합니다:
```
☑ 홍길동 (가정)
☑ 김영희 (직장)
☐ 박민수
☑ 이순신 (기도팀)
☐ 장보고
```

### 4. 기도제목 선택 예제

자동으로 선택된 대상자의 기도제목만 표시:
```
☑ 가정의 평안 (홍길동)
☑ 사역의 축복 (홍길동)
☑ 자녀 교육 (홍길동)
☑ 직장의 승진 (김영희)
☑ 동료와의 관계 (김영희)
☑ 신앙의 성장 (이순신)
... (더 많은 항목)
```

### 5. 백업 실행

사용자가 "백업" 버튼 클릭:
```
대상자 3명과 기도제목 6개가 선택되었습니다
→ [백업] 버튼 활성화

클릭 후:
[백업 중...] ← 진행 상황 표시
```

### 6. 완료 및 알림

```
✓ Firebase 백업이 완료되었습니다 (ID: a1b2c3d4e5f6...)

→ 다이얼로그 자동 닫힘
→ Settings 화면으로 돌아감
```

## 코드 예제

### ViewModel에서 백업 실행

```kotlin
fun backupToFirebase() {
    if (_selectedPersons.value.isEmpty()) {
        viewModelScope.launch {
            _uiEvent.emit(SettingsEvent.Error("백업할 대상자를 선택해주세요"))
        }
        return
    }

    viewModelScope.launch {
        try {
            _backupInProgress.value = true
            val result = backupRepository.backupSelectedData(
                persons = _selectedPersons.value,           // 선택된 대상자
                topics = _selectedTopics.value,             // 선택된 기도제목
                includeAnswered = true                      // 답변된 기도도 포함
            )

            result.onSuccess { backupId ->
                Log.d("SettingsViewModel", "Backup successful: $backupId")
                _selectedPersons.value = emptyList()
                _selectedTopics.value = emptyList()
                _uiEvent.emit(SettingsEvent.FirebaseBackupSuccess(backupId))
            }.onFailure { exception ->
                Log.e("SettingsViewModel", "Backup failed", exception)
                _uiEvent.emit(SettingsEvent.Error(exception.message ?: "Firebase 백업 실패"))
            }
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Exception during backup", e)
            _uiEvent.emit(SettingsEvent.Error(e.message ?: "Firebase 백업 중 오류 발생"))
        } finally {
            _backupInProgress.value = false
        }
    }
}
```

### Firebase Firestore에 저장되는 데이터 구조

```json
{
  "backups": {
    "user123abc": {
      "sessions": {
        "backup_20240101_120000": {
          "id": "backup_20240101_120000",
          "userId": "user123abc",
          "personBackupIds": [
            "person_doc_1",
            "person_doc_2",
            "person_doc_3"
          ],
          "topicBackupIds": [
            "topic_doc_1",
            "topic_doc_2",
            "topic_doc_3",
            "topic_doc_4",
            "topic_doc_5",
            "topic_doc_6"
          ],
          "totalPersons": 3,
          "totalTopics": 6,
          "timestamp": "2024-01-01T12:00:00Z",
          "status": "COMPLETED",
          "persons": {
            "person_doc_1": {
              "id": "person_doc_1",
              "originalId": 1,
              "name": "홍길동",
              "memo": "가정",
              "colorTag": "#FF5722",
              "createdAt": "2023-12-15T10:30:00Z",
              "updatedAt": "2023-12-25T14:45:00Z",
              "backupTimestamp": "2024-01-01T12:00:00Z"
            },
            "person_doc_2": {
              "id": "person_doc_2",
              "originalId": 2,
              "name": "김영희",
              "memo": "직장",
              "colorTag": "#2196F3",
              "createdAt": "2023-11-20T09:00:00Z",
              "updatedAt": "2023-12-28T16:20:00Z",
              "backupTimestamp": "2024-01-01T12:00:00Z"
            }
          },
          "topics": {
            "topic_doc_1": {
              "id": "topic_doc_1",
              "originalId": 10,
              "personId": 1,
              "personBackupId": "person_doc_1",
              "title": "가정의 평안",
              "priority": 3,
              "status": "ACTIVE",
              "createdAt": "2023-12-15T10:30:00Z",
              "answeredAt": null,
              "backupTimestamp": "2024-01-01T12:00:00Z"
            },
            "topic_doc_2": {
              "id": "topic_doc_2",
              "originalId": 11,
              "personId": 1,
              "personBackupId": "person_doc_1",
              "title": "사역의 축복",
              "priority": 2,
              "status": "ACTIVE",
              "createdAt": "2023-12-16T11:15:00Z",
              "answeredAt": null,
              "backupTimestamp": "2024-01-01T12:00:00Z"
            }
          }
        }
      }
    }
  }
}
```

## 사용자 시나리오

### 시나리오 1: 모든 데이터 백업

1. Settings → "Firebase 백업" 클릭
2. "대상자 선택" 섹션에서 "모두 선택" 클릭
3. "기도제목 선택" 섹션에서 "모두 선택" 클릭
4. "백업" 버튼 클릭
5. 모든 대상자와 기도제목이 Firebase에 백업됨

### 시나리오 2: 선택적 백업

1. Settings → "Firebase 백업" 클릭
2. 특정 대상자만 체크박스 클릭하여 선택
3. 해당 대상자의 기도제목만 표시됨
4. 필요한 기도제목만 선택
5. "백업" 버튼 클릭
6. 선택된 데이터만 Firebase에 백업됨

### 시나리오 3: 백업 취소

1. Settings → "Firebase 백업" 클릭
2. 항목 선택
3. "취소" 버튼 클릭
4. 다이얼로그 닫힘
5. 아무 데이터도 백업되지 않음

## 오류 처리 예제

### 대상자를 선택하지 않은 경우
```
사용자: 아무것도 선택하지 않고 "백업" 버튼 클릭
결과: "백업할 대상자를 선택해주세요" 에러 메시지 표시
```

### 네트워크 오류
```
사용자: 인터넷 연결이 없는 상태에서 "백업" 버튼 클릭
결과: "Firebase 백업 실패" 에러 메시지 표시
```

### Firebase 인증 오류
```
사용자: Firebase 인증이 되지 않은 상태
결과: "사용자가 로그인하지 않았습니다" 에러 메시지 표시
```

## 성공 사례

✅ **기도 기록 보존**
- 중요한 기도 주제들을 안전하게 저장
- 클라우드에서 언제든지 접근 가능

✅ **기기 변경 시 데이터 이전**
- 새로운 기기에서도 백업된 데이터 확인 가능
- 기존 데이터 손실 방지

✅ **선택적 백업**
- 필요한 대상자와 기도제목만 백업
- 개인정보 보호

✅ **자동 타임스탐프**
- 언제 백업되었는지 명확히 기록
- 여러 백업 버전 관리 가능
