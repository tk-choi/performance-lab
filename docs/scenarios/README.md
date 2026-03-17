# 시나리오 분석 노트

이 디렉토리는 각 Lab의 **before/after 분석 결과를 기록하는 공간**입니다.
Lab을 진행하면서 관찰한 증상, 원인 분석 내용, 개선 전후 비교 결과를 아래 템플릿에 맞춰 작성하세요.

---

## 파일명 규칙

```
{lab}-{시나리오-설명}.md
```

**예시**

| 파일명 | 대상 Lab | 시나리오 |
|--------|---------|---------|
| `sql-full-table-scan.md` | SQL Lab | Full Table Scan 재현 및 인덱스 적용 |
| `sql-n-plus-one.md` | SQL Lab | N+1 문제 재현 및 fetch join 적용 |
| `thread-tomcat-exhaustion.md` | Thread Lab | Tomcat Thread 고갈 재현 |
| `thread-hikari-timeout.md` | Thread Lab | HikariCP 커넥션 고갈 재현 |
| `cache-ttl-miss.md` | Cache Lab | TTL 과소 설정으로 인한 Cache Miss |
| `cache-stampede.md` | Cache Lab | Cache Stampede 재현 |
| `gc-memory-leak.md` | GC Lab | static 컬렉션 메모리 누수 |
| `gc-full-gc-trigger.md` | GC Lab | Full GC 유발 패턴 |

---

## 분석 노트 Markdown 템플릿

아래 템플릿을 복사해 새 파일을 만드세요.

```markdown
# {시나리오명}

## 재현 환경

| 항목 | 내용 |
|------|------|
| 날짜 | YYYY-MM-DD |
| Lab | SQL / Thread / Cache / GC |
| Lab 버전 | (커밋 해시 또는 브랜치명) |
| JDK | 17+ |
| Spring Boot | 3.x |

---

## 증상

before 상태에서 관찰된 증상을 기술합니다.

- **엔드포인트**: `GET /sql/users/search?name=홍길동`
- **응답 시간**: 약 N초
- **에러 여부**: 있음 / 없음
- **관찰 내용**:
  - (예) 응답이 5초 이상 소요됨
  - (예) `tomcat.threads.busy` 메트릭이 최대값에 도달

---

## 원인 분석

### 분석 도구

- [ ] EXPLAIN ANALYZE
- [ ] Spring Actuator 메트릭
- [ ] redis-cli INFO stats
- [ ] GC 로그
- [ ] Thread Dump

### 분석 결과

```
(분석 도구 실행 결과 붙여넣기)
```

**결론**: (원인을 1-3문장으로 요약)

---

## 개선 방법

- (예) `name` 컬럼에 인덱스 추가 (`CREATE INDEX idx_user_name ON users(name)`)
- (예) `Thread.sleep()` 제거 및 비동기 처리로 전환
- (예) TTL을 1초에서 60초로 조정
- (예) `-Xmx` 힙 크기 조정 및 static 컬렉션 참조 제거

---

## Before / After 비교

| 메트릭 | Before | After | 개선율 |
|--------|--------|-------|--------|
| 응답시간 (p95) | N ms | N ms | -N% |
| 에러율 | N% | N% | -N%p |
| TPS | N | N | +N% |
| (추가 메트릭) | - | - | - |

---

## 결론 및 학습 포인트

- **핵심 원인**:
- **해결 전략**:
- **학습한 점**:
  - (예) `EXPLAIN` 결과에서 `type: ALL`은 Full Table Scan을 의미한다.
  - (예) HikariCP `connectionTimeout` 초과 시 `SQLTransientConnectionException`이 발생한다.
- **추가 실험 아이디어**:
```

---

## 작성된 분석 노트 목록

| 파일 | Lab | 시나리오 | 작성일 |
|------|-----|---------|--------|
| (작성 후 이 표에 추가하세요) | | | |
