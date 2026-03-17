# Performance Lab — 프로젝트 요약

## 프로젝트 개요

**목적**: Kotlin/Spring 기반 백엔드 성능 문제를 직접 재현하고 디버깅하는 학습용 샌드박스  
**스택**: JDK 17+, Spring Boot 3.x, MySQL 8, Redis 7, Docker Compose  
**학습 방식**: 문제 재현 → 증상 확인 → 원인 분석 → 개선 → before/after 비교

---

## 디렉토리 구조

```
performance-lab/
├── docker-compose.yml
├── build.gradle
├── src/main/java/com/lab/
│   ├── sql/          # SQL Execution Plan Lab
│   ├── thread/       # Thread Pool Lab
│   ├── cache/        # Redis Cache Miss Lab
│   └── gc/           # GC Lab
├── scripts/
│   ├── seed/         # 100만 건 데이터 시드 스크립트
│   └── load-test/    # k6 부하 테스트 스크립트
└── docs/
    └── scenarios/    # 시나리오별 분석 노트
```

---

## 구현 우선순위 및 시나리오

### 1순위 — SQL Execution Plan Lab (`/sql`)

**목표**: 실행계획을 읽고, 나쁜 쿼리를 직접 개선하는 감각 익히기

**재현 시나리오**

| 시나리오 | 엔드포인트 | 핵심 포인트 |
|----------|-----------|------------|
| Full Table Scan | `GET /sql/users/search?name=` | 인덱스 없는 컬럼 조회 |
| 인덱스 무력화 | `GET /sql/orders/by-year?year=` | `FUNCTION()` 감싸기, LIKE `%keyword` |
| N+1 문제 | `GET /sql/posts/with-comments` | JPA `@OneToMany` Lazy Loading |
| 복합 인덱스 순서 | `GET /sql/logs/search` | 컬럼 순서에 따른 실행계획 차이 |

**구현 체크리스트**
- [ ] `User`, `Order`, `Post`, `Comment`, `Log` 엔티티 설계
- [ ] 100만 건 시드 데이터 스크립트 작성 (`scripts/seed/`)
- [ ] 각 엔드포인트에서 `EXPLAIN ANALYZE` 결과를 응답에 함께 반환
- [ ] 나쁜 쿼리 버전(before) / 개선 버전(after) 컨트롤러 분리
- [ ] 인덱스 DDL 스크립트 작성 (`schema.sql`)

**분석 기준**: `type` 컬럼 — `ALL` → `index` → `range` → `ref` → `const` 순으로 개선

---

### 2순위 — Thread Pool Lab (`/thread`)

**목표**: Thread/Connection Pool 고갈을 재현하고 Actuator 메트릭으로 모니터링

**재현 시나리오**

| 시나리오 | 엔드포인트 | 핵심 포인트 |
|----------|-----------|------------|
| Tomcat Thread 고갈 | `GET /thread/slow-api` | `Thread.sleep(5000)` 으로 점유 |
| `@Async` Queue 적체 | `POST /thread/async-task` | `ThreadPoolTaskExecutor` 큐 초과 |
| HikariCP 고갈 | `GET /thread/db-heavy` | DB 커넥션 점유 후 timeout 재현 |
| Thread/DB Pool 불균형 | 부하 테스트 시나리오 | thread max > hikari max 설정 차이 |

**구현 체크리스트**
- [ ] `application.yml`에 의도적으로 낮은 pool 사이즈 설정 (thread: 5, hikari: 3)
- [ ] `/actuator/metrics/tomcat.threads.busy` 응답 확인
- [ ] `/actuator/metrics/hikaricp.connections.active` 응답 확인
- [ ] `@Async` 설정용 `ThreadPoolConfig.java` 작성
- [ ] k6 동시 100 요청 스크립트 (`scripts/load-test/thread-test.js`)

**분석 기준**: `active threads` / `queue size` / `connection wait time` 상관관계

---

### 3순위 — Redis Cache Miss Lab (`/cache`)

**목표**: Cache Miss 원인을 코드 레벨에서 파악하고 패턴별 해결책 체험

**재현 시나리오**

| 시나리오 | 엔드포인트 | 핵심 포인트 |
|----------|-----------|------------|
| TTL 과소 설정 | `GET /cache/products/{id}` | TTL 1초로 설정, Miss율 확인 |
| Cache Stampede | `GET /cache/popular-items` | 동시 요청 + TTL 만료 타이밍 |
| Self-invocation 함정 | `GET /cache/orders/{id}` | 같은 클래스 내 `@Cacheable` 미적용 |
| Key 설계 문제 | `GET /cache/search?q=&page=` | 파라미터 조합에 따른 Key 폭발 |

**구현 체크리스트**
- [ ] `CacheMetricsAspect.java` — Hit/Miss 카운터 AOP로 구현
- [ ] `/cache/stats` 엔드포인트 — hit율, miss율 반환
- [ ] Self-invocation 예시 코드 (`CacheService.java` 내 bad/good 버전)
- [ ] `redis-cli INFO stats` 명령어 결과 비교 문서 작성
- [ ] TTL 설정별 Miss율 비교 테스트

**분석 기준**: `redis-cli INFO stats`의 `keyspace_hits` / `keyspace_misses` 비율

---

### 4순위 — GC Lab (`/gc`)

**목표**: GC 로그를 읽고, 메모리 누수와 Full GC 유발 패턴 체험

**재현 시나리오**

| 시나리오 | 엔드포인트 | 핵심 포인트 |
|----------|-----------|------------|
| 메모리 누수 | `GET /gc/leak` | `static List`에 1MB씩 누적 |
| Full GC 유발 | `GET /gc/large-object` | 대용량 객체 반복 생성 |
| String 과다 생성 | `GET /gc/string-concat` | `+` 연산 vs `StringBuilder` |

**구현 체크리스트**
- [ ] JVM 옵션 설정 (`-Xlog:gc* -Xmx256m`)
- [ ] `/gc/heap-status` 엔드포인트 — 현재 heap 사용량 반환
- [ ] VisualVM 또는 JConsole 연결 가이드 (`docs/gc-monitoring.md`)
- [ ] GC 로그 샘플 파일 첨부 (`docs/scenarios/gc-log-sample.txt`)

---

## 공통 인프라 설정

### Docker Compose
```yaml
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_DATABASE: perflab
  redis:
    image: redis:7-alpine
    command: redis-server --appendonly yes
  app:
    build: .
    depends_on: [mysql, redis]
    ports: ["8080:8080"]
```

### Spring Actuator 노출 엔드포인트
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, metrics, threaddump, heapdump
```

### 모니터링 (선택)
- Prometheus + Grafana (`docker-compose.monitoring.yml` 별도 분리)
- 대시보드: Thread active/queue, HikariCP, JVM heap, Redis hit율

---

## 학습 흐름 (각 시나리오 공통)

```
1. before 엔드포인트 호출
2. 증상 확인 (응답 느림 / 에러 / 메트릭 이상)
3. 원인 분석 (EXPLAIN / Actuator / redis-cli / GC 로그)
4. after 코드로 교체 또는 설정 변경
5. 동일 부하로 재측정 → before/after 비교 기록
```

---

## Git 브랜치 전략

```
main
├── feature/sql-lab
├── feature/thread-lab
├── feature/cache-lab
└── feature/gc-lab
```

각 Lab마다 `before` 커밋과 `after` 커밋을 명확히 분리해서 diff로 학습 포인트를 확인할 수 있도록 관리.

---

## 참고 명령어 모음

```bash
# 데이터 시드
./gradlew bootRun --args='--spring.profiles.active=seed'

# 부하 테스트
k6 run scripts/load-test/thread-test.js

# Redis 모니터링
redis-cli INFO stats | grep keyspace
redis-cli MONITOR

# GC 로그 확인 (JVM 옵션 추가 후)
tail -f logs/gc.log

# Thread Dump
curl http://localhost:8080/actuator/threaddump | jq
```