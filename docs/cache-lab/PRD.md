# Cache Lab PRD

## 목적 및 학습 목표

- Cache Miss 원인을 코드 레벨에서 파악하고 패턴별 해결책 체험
- Redis Hit/Miss 비율을 `redis-cli INFO stats`로 모니터링하는 능력
- Spring AOP 프록시 기반 `@Cacheable`의 Self-invocation 함정 이해
- TTL 과소 설정, Cache Stampede, Key 설계 문제의 원인과 해결책 체득

---

## 재현 시나리오

### 시나리오 1. TTL 과소 설정

**엔드포인트**: `GET /cache/products/{id}`

| 구분 | 상태 | 설명 |
|------|------|------|
| Before | 문제 있는 상태 | TTL=1초로 설정 → 대부분의 요청이 Cache Miss → DB 부하 급증 |
| After | 개선 상태 | 적절한 TTL 설정 (예: 60초) → Hit율 대폭 향상 |

- **Before 증상**: TTL이 1초에 불과해 캐시가 거의 즉시 만료되므로 반복 요청마다 DB를 조회
- **After 효과**: TTL을 60초로 늘리면 동일 상품에 대한 반복 요청은 Redis에서 응답
- **관찰**: `redis-cli INFO stats`의 `keyspace_hits` / `keyspace_misses` 비율 변화

> products 엔티티 데이터 모델은 SQL Lab의 데이터 모델을 공유함 (→ `../sql-lab/ERD.md` 참조).

---

### 시나리오 2. Cache Stampede

**엔드포인트**: `GET /cache/popular-items`

| 구분 | 상태 | 설명 |
|------|------|------|
| Before | 문제 있는 상태 | TTL 만료 직후 동시 요청이 모두 DB 조회 → 동일 데이터를 여러 번 DB에서 조회 |
| After | 개선 상태 | Mutex Lock 패턴 또는 Probabilistic Early Expiration 적용 |

- **Before 증상**: 캐시 만료 순간 수십 개의 스레드가 동시에 DB를 조회하고 동일한 데이터를 Redis에 중복 저장
- **After 효과**: 첫 번째 스레드만 DB를 조회하고 나머지는 락 해제 후 Redis에서 응답
- **관찰**: TTL 만료 순간의 DB 쿼리 급증 패턴 — `redis-cli MONITOR`로 실시간 확인 가능

**동시 요청 재현 방법**:

```bash
# ab(Apache Bench)를 사용해 TTL 만료 직후 50개의 동시 요청 발생
ab -n 50 -c 50 http://localhost:8080/cache/popular-items

# 또는 k6 스크립트로 1초 내 50 VU 동시 요청
k6 run --vus 50 --duration 1s scripts/load-test/cache-stampede.js
```

캐시가 만료될 때 타이밍을 맞추려면 TTL 만료 직전에 위 명령을 실행한다. 이때 `redis-cli MONITOR`를 병렬로 실행하면 DB 쿼리 급증 패턴을 실시간으로 확인할 수 있다.

---

### 시나리오 3. Self-invocation 함정

**엔드포인트**: `GET /cache/orders/{id}`

| 구분 | 상태 | 설명 |
|------|------|------|
| Before | 문제 있는 상태 | 같은 클래스 내에서 `@Cacheable` 메서드를 직접 호출 → Spring AOP 프록시 우회 → 캐시 미적용 |
| After | 개선 상태 | 별도 빈(Bean)으로 분리하거나 `ApplicationContext`를 통해 자기 자신 주입 |

- **Before 증상**: `this.getOrder(id)` 형태의 내부 호출은 Spring AOP 프록시를 거치지 않으므로 `@Cacheable`이 동작하지 않음. `/cache/stats` 엔드포인트의 miss 카운터가 요청마다 계속 증가
- **After 효과**: 별도 빈으로 분리(`CacheQueryService.kt`)하거나 `self` 주입 방식으로 프록시를 경유하여 캐시 정상 적용
- **관찰**: `/cache/stats` 엔드포인트의 hit 카운터 변화 (before에서는 hit 없음, after에서는 hit 발생)

**원인 설명**:

Spring의 `@Cacheable`은 AOP 프록시 기반으로 동작한다. 외부에서 빈을 통해 메서드를 호출하면 프록시가 개입하여 캐시 로직을 실행하지만, 같은 클래스 내부의 `this.method()` 호출은 프록시를 우회하므로 캐시가 적용되지 않는다.

```
외부 호출: Client → Proxy → @Cacheable 동작 → 캐시 적용 O
내부 호출: this.getOrder(id) → Proxy 우회 → @Cacheable 무시 → 캐시 적용 X
```

> orders 엔티티 데이터 모델은 SQL Lab의 데이터 모델을 공유함 (→ `../sql-lab/ERD.md` 참조).

---

### 시나리오 4. Key 설계 문제

**엔드포인트**: `GET /cache/search?q=&page=`

| 구분 | 상태 | 설명 |
|------|------|------|
| Before | 문제 있는 상태 | q와 page 조합으로 무한한 캐시 Key 생성 → Redis 메모리 급증, Key 폭발 |
| After | 개선 상태 | Key 정규화 (예: 페이지 파라미터 제외 또는 Key 제한 정책 적용) |

- **Before 증상**: 검색어 수천 개 × 페이지 번호 조합으로 Redis Key가 폭발적으로 증가하며 메모리 한계에 도달
- **After 효과**: Key 정규화 또는 페이지 범위 제한 정책으로 Key 수를 통제 가능한 범위로 관리
- **관찰**: `redis-cli DBSIZE` 명령으로 Key 수 증가 추이 확인

```bash
# Key 수 모니터링
watch -n 1 'redis-cli DBSIZE'

# 패턴 기반 Key 목록 확인
redis-cli KEYS "cache:search:*" | wc -l
```

---

## API 엔드포인트 정의

| 메서드 | 경로 | 파라미터 | 설명 |
|--------|------|---------|------|
| GET | `/cache/products/{id}` | `id`: Long | 상품 조회 (TTL 실험) |
| GET | `/cache/popular-items` | - | 인기 상품 목록 (Stampede 실험) |
| GET | `/cache/orders/{id}` | `id`: Long | 주문 조회 (Self-invocation 실험) |
| GET | `/cache/search` | `q`: String, `page`: Int | 검색 (Key 설계 실험) |
| GET | `/cache/stats` | - | Hit/Miss 카운터 통계 조회 |

각 엔드포인트는 before/after 버전을 별도 경로로 제공한다.

- 예: `/cache/products/{id}/before`, `/cache/products/{id}/after`

products와 orders 엔티티는 SQL Lab의 데이터 모델을 공유함 (→ `../sql-lab/ERD.md` 참조).

---

## 구현 체크리스트

- [ ] `CacheMetricsAspect.kt` — AOP로 Hit/Miss 카운터 구현
- [ ] `/cache/stats` 엔드포인트 — hit율, miss율 반환
- [ ] `CacheService.kt` 내 bad(Self-invocation) / good(별도 Bean) 버전 코드
- [ ] `CacheQueryService.kt` — Self-invocation 해결용 별도 빈 분리
- [ ] `redis-cli INFO stats` 명령어 결과 비교 문서 작성
- [ ] TTL 설정별 Miss율 비교 테스트 (1초 vs 60초)
- [ ] k6 동시 요청 스크립트 (`scripts/load-test/cache-stampede.js`) — Stampede 재현용
- [ ] `application.yml`에 캐시 TTL 설정 항목 분리 (before/after 프로파일)

---

## 성공 지표 (분석 기준)

| 지표 | Before (문제 상태) | After (개선 상태) |
|------|-------------------|------------------|
| `keyspace_hits` / (`hits` + `misses`) | < 30% | > 80% |
| `/cache/stats` hit율 | 낮음 (hit 거의 없음) | > 80% |
| DB 쿼리 수 (같은 요청 반복 시) | 매 요청마다 DB 조회 | 캐시 히트 시 0 |
| `redis-cli DBSIZE` (Key 설계 문제) | 무한 증가 | 설계 범위 내 안정 |

- 분석 기준: `redis-cli INFO stats`의 `keyspace_hits` / `keyspace_misses` 비율

```bash
# Redis 통계 확인 명령
redis-cli INFO stats | grep -E 'keyspace_hits|keyspace_misses'

# Hit율 계산
# Hit율 = keyspace_hits / (keyspace_hits + keyspace_misses) × 100
```

---

## 관련 문서 링크

- `../sql-lab/ERD.md` — products, orders 엔티티 데이터 모델 (Cache Lab과 공유)
- `../DEVELOP_SETTING_GUIDE.md#cache-lab-설정` — Redis CLI 명령어 및 모니터링 방법
- `../scenarios/README.md` — 분석 결과 기록 템플릿
- `../README.md` — 전체 학습 경로
