# 개발 환경 설정 가이드

Performance Lab 전체 Lab에서 공통으로 사용하는 개발 환경 구성 방법을 설명합니다.

---

## 공통 설정

### 사전 요구사항

| 도구 | 버전 | 설치 확인 명령어 |
|------|------|----------------|
| JDK | 17 이상 | `java -version` |
| Docker Desktop | 최신 | `docker --version` |
| k6 | 최신 | `k6 version` |
| Redis CLI | 최신 | `redis-cli --version` |

### Docker Compose 실행

```bash
docker-compose up -d
```

모든 서비스(MySQL, Redis, App)가 백그라운드로 시작됩니다.

### 애플리케이션 실행

```bash
./gradlew bootRun
```

### Spring Actuator 헬스 확인

```bash
curl http://localhost:8080/actuator/health
```

정상 응답 예시: `{"status":"UP"}`

### 포트 정보

| 서비스 | 포트 |
|--------|------|
| 애플리케이션 (Spring Boot) | 8080 |
| MySQL | 3306 |
| Redis | 6379 |

---

## SQL Lab 설정 {#sql-lab-설정}

### 시드 데이터 투입 (100만 건)

```bash
./gradlew bootRun --args='--spring.profiles.active=seed'
```

> 처음 한 번만 실행하면 됩니다. 완료까지 수 분이 소요될 수 있습니다.

### MySQL 클라이언트 접속

```bash
docker exec -it <container> mysql -u root -p perflab
```

컨테이너 이름은 `docker ps` 명령어로 확인합니다.

### EXPLAIN ANALYZE 실행 방법

MySQL 클라이언트 접속 후 실행:

```sql
EXPLAIN ANALYZE SELECT * FROM users WHERE name = '홍길동';
```

결과의 `type` 컬럼 기준 개선 방향: `ALL` → `index` → `range` → `ref` → `const`

### 엔드포인트 호출 예시

```bash
# Full Table Scan 재현
curl "http://localhost:8080/sql/users/search?name=홍길동"

# 인덱스 무력화 재현
curl "http://localhost:8080/sql/orders/by-year?year=2024"

# N+1 문제 재현
curl "http://localhost:8080/sql/posts/with-comments"

# 복합 인덱스 순서 비교
curl "http://localhost:8080/sql/logs/search"
```

---

## Thread Lab 설정 {#thread-lab-설정}

### k6 설치 방법

**Mac (Homebrew)**:

```bash
brew install k6
```

**기타 환경**: [k6 공식 문서](https://grafana.com/docs/k6/latest/set-up/install-k6/) 참고

### Actuator 메트릭 확인

**Tomcat 스레드 사용량**:

```bash
curl http://localhost:8080/actuator/metrics/tomcat.threads.busy
```

**HikariCP 커넥션 사용량**:

```bash
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
```

### Thread Dump 수집

```bash
curl http://localhost:8080/actuator/threaddump | jq
```

> `jq`가 없는 경우 `brew install jq`로 설치하세요.

### k6 부하 테스트 실행

```bash
k6 run scripts/load-test/thread-test.js
```

동시 100 요청으로 Thread Pool 고갈 상태를 재현합니다.

---

## Cache Lab 설정 {#cache-lab-설정}

### Redis CLI 접속

```bash
docker exec -it <container> redis-cli
```

컨테이너 이름은 `docker ps` 명령어로 확인합니다.

### 주요 Redis CLI 명령어

**Hit/Miss 통계 확인**:

```bash
redis-cli INFO stats | grep keyspace
```

출력 예시:
```
keyspace_hits:1523
keyspace_misses:847
```

**실시간 명령어 모니터링**:

```bash
redis-cli MONITOR
```

> `MONITOR`는 운영 환경에서 사용 금지. 학습 목적으로만 사용하세요.

### Hit/Miss 비율 계산

```
Hit율 = keyspace_hits / (keyspace_hits + keyspace_misses) × 100
```

### Cache 통계 엔드포인트

```bash
curl http://localhost:8080/cache/stats
```

AOP로 수집한 Hit/Miss 카운터와 비율을 JSON으로 반환합니다.

---

## GC Lab 설정 {#gc-lab-설정}

### JVM 옵션 설정 방법

**application.yml**:

```yaml
spring:
  jvm:
    options: -Xlog:gc* -Xmx256m
```

**환경 변수 (JAVA_OPTS)**:

```bash
JAVA_OPTS="-Xlog:gc* -Xmx256m" ./gradlew bootRun
```

> `-Xmx256m`은 의도적으로 낮게 설정해 Full GC를 빠르게 유발합니다.

### VisualVM 연결 절차

1. VisualVM 설치: [visualvm.github.io](https://visualvm.github.io/)
2. `application.yml` 또는 JAVA_OPTS에 JMX 옵션 추가:

```
-Dcom.sun.management.jmxremote
-Dcom.sun.management.jmxremote.port=9999
-Dcom.sun.management.jmxremote.authenticate=false
-Dcom.sun.management.jmxremote.ssl=false
```

3. VisualVM 실행 → **File > Add JMX Connection** → `localhost:9999` 입력
4. **Monitor** 탭에서 Heap 사용량 및 GC 활동을 실시간 확인

### GC 로그 확인

```bash
tail -f logs/gc.log
```

### Heap 상태 엔드포인트

```bash
curl http://localhost:8080/gc/heap-status
```

현재 Heap 사용량(used/max)과 GC 횟수를 JSON으로 반환합니다.
