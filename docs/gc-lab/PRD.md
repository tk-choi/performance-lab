# GC Lab PRD

## 목적 및 학습 목표

- GC 로그를 읽고 메모리 누수와 Full GC 유발 패턴을 직접 체험
- JVM 힙 구조(Eden → Survivor → Old Gen)와 GC 동작 원리 이해
- VisualVM/JConsole을 통한 JVM 모니터링 능력
- `static` 컬렉션의 위험성과 객체 수명 주기 관리 감각 익히기

---

## 재현 시나리오

### 시나리오 1. 메모리 누수

**엔드포인트**: `GET /gc/leak`

| 구분 | 상태 | 설명 |
|------|------|------|
| Before | 문제 있는 상태 | `static List<byte[]>`에 1MB씩 계속 누적 → 참조 반환 불가 → Old Gen 가득 → Full GC 반복 → OOM |
| After | 개선 상태 | 요청 처리 후 리스트 초기화 (참조 제거) → 정상 GC 동작 |

- **Before 증상**: static 필드는 클래스 로더가 살아있는 한 GC 대상이 되지 않는다. 매 요청마다 1MB 배열을 리스트에 추가하면 Old Gen이 점차 가득 차고 Full GC가 반복되다 결국 `OutOfMemoryError`가 발생한다.
- **After 효과**: 요청 처리가 끝난 후 리스트를 초기화하거나 인스턴스 스코프로 전환하면 참조가 해제되어 다음 GC 사이클에서 정상적으로 회수된다.
- **관찰**: `GET /gc/heap-status`로 Heap 사용량 추이 확인, GC 로그에서 `GC cause: Allocation Failure` 발생 빈도 모니터링

---

### 시나리오 2. Full GC 유발

**엔드포인트**: `GET /gc/large-object`

| 구분 | 상태 | 설명 |
|------|------|------|
| Before | 문제 있는 상태 | 매 요청마다 수십 MB 크기 배열 생성 → Young Gen 수용 불가 → Old Gen 직접 할당 → Full GC 빈발 |
| After | 개선 상태 | 객체 풀링(Pool) 또는 스트리밍 처리로 대용량 객체 생성 최소화 |

- **Before 증상**: JVM은 Young Gen(Eden)에 할당할 수 없을 만큼 큰 객체를 Old Gen에 직접 할당한다. `-Xmx256m` 제한 환경에서 대용량 배열을 반복 생성하면 `Pause Full` GC가 빈발하고 응답 지연이 눈에 띄게 발생한다.
- **After 효과**: 객체 재사용 풀링 또는 스트리밍 방식으로 전환하면 Old Gen 직접 할당이 줄어들어 Full GC 발생 빈도가 크게 감소한다.
- **관찰**: GC 로그의 `Pause Full` 발생 횟수와 pause time 비교

---

### 시나리오 3. String 과다 생성

**엔드포인트**: `GET /gc/string-concat`

| 구분 | 상태 | 설명 |
|------|------|------|
| Before | 문제 있는 상태 | 반복문에서 `+` 연산으로 String 연결 → 매 반복마다 새 String 객체 생성 → Young GC 빈발 |
| After | 개선 상태 | `StringBuilder` 사용 → 객체 생성 최소화 → GC 부담 감소 |

- **Before 증상**: `String`은 불변(immutable) 객체이므로 `+` 연산을 N번 수행하면 N개의 중간 String 객체가 생성된다. Eden이 빠르게 채워지며 Young GC가 빈번히 발생한다.
- **After 효과**: `StringBuilder`는 내부 버퍼를 재사용하므로 중간 객체 생성이 없다. Young GC 발생 빈도가 크게 줄어든다.
- **관찰**: Young GC 발생 빈도 비교 (before vs after)

---

## API 엔드포인트 정의

| 메서드 | 경로 | 설명 | 기대 증상 (Before) |
|--------|------|------|-------------------|
| GET | `/gc/leak` | static 컬렉션에 1MB씩 누적 | Heap 사용량 지속 증가, 결국 OOM |
| GET | `/gc/leak/reset` | 누수 유발 리스트 초기화 | - |
| GET | `/gc/large-object` | 대용량 배열 반복 생성 | Full GC 빈발, 응답 지연 |
| GET | `/gc/string-concat` | `+` 연산 String 연결 (N회) | Young GC 빈발 |
| GET | `/gc/heap-status` | 현재 Heap 사용량 조회 | - |

각 엔드포인트는 before/after 버전을 별도 경로로 제공한다.

- 예: `/gc/leak/before`, `/gc/leak/after`

누수 유발(`/gc/leak`)과 초기화(`/gc/leak/reset`)는 엔드포인트를 분리하여 실험 재현성을 높인다.

---

## 구현 체크리스트

- [ ] JVM 옵션 설정: `-Xlog:gc* -Xmx256m` — `-Xmx256m`은 힙을 의도적으로 제한하여 GC 현상을 짧은 시간 안에 재현하기 위한 설정
- [ ] `/gc/heap-status` 엔드포인트 — 현재 Heap 사용량(used/max) 반환
- [ ] `GcLeakController.kt` — `/gc/leak/before`, `/gc/leak/after`, `/gc/leak/reset` 엔드포인트
- [ ] `GcLargeObjectController.kt` — `/gc/large-object/before`, `/gc/large-object/after` 엔드포인트
- [ ] `GcStringConcatController.kt` — `/gc/string-concat/before`, `/gc/string-concat/after` 엔드포인트
- [ ] VisualVM 또는 JConsole 연결 가이드 (`../DEVELOP_SETTING_GUIDE.md#gc-lab-설정` 참조)
- [ ] GC 로그 샘플 파일 첨부 (`../scenarios/gc-log-sample.txt`)
- [ ] 메모리 누수 시나리오의 leak 유발/리셋 엔드포인트 분리

---

## 성공 지표 (분석 기준)

| 지표 | Before (문제 상태) | After (개선 상태) |
|------|-------------------|------------------|
| Heap 사용률 (`/gc/heap-status`) | 지속 증가 (누수) | 일정 수준 유지 |
| GC 로그 `Pause Full` 발생 | 빈번 (>10회/분) | 없음 또는 희소 |
| Young GC pause time (String 시나리오) | 높음 | 감소 |
| GC 로그 주요 필드 해석 | - | GC cause / pause time / heap 변화 설명 포함 |

**GC 로그 읽는 법** (간단 예시):

```
[0.012s][info][gc] GC(0) Pause Young (Allocation Failure) 8M->1M(256M) 3.456ms
                                        ↑ 원인          ↑ before→after  ↑ pause time
```

- `Allocation Failure`: Eden 공간이 부족해 객체를 할당할 수 없어 GC가 발생했음을 의미
- `8M->1M(256M)`: GC 전 8MB → GC 후 1MB (전체 힙 256MB)
- `3.456ms`: 애플리케이션 스레드가 멈춘 STW(Stop-The-World) 시간

---

## 관련 문서 링크

- `../DEVELOP_SETTING_GUIDE.md#gc-lab-설정` — JVM 옵션 설정 및 VisualVM 연결 방법
- `../scenarios/README.md` — GC 로그 분석 결과 기록 템플릿
- `../scenarios/gc-log-sample.txt` — GC 로그 샘플 (구현 후 추가 예정)
- `../README.md` — 전체 학습 경로
