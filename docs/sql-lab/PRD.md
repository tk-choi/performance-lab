# SQL Lab PRD

## 목적 및 학습 목표

- SQL 실행계획(EXPLAIN ANALYZE)을 읽고 나쁜 쿼리를 직접 개선하는 감각 익히기
- EXPLAIN의 type 컬럼(ALL → index → range → ref → const) 의미 이해
- JPA N+1 문제를 직접 재현하고 Fetch Join으로 해결하는 경험

---

## 재현 시나리오

### 시나리오 1. Full Table Scan

**엔드포인트**: `GET /sql/users/search?name=`

| 구분 | 상태 | 설명 |
|------|------|------|
| Before | 문제 있는 상태 | 인덱스 없는 `name` 컬럼으로 조회 → `EXPLAIN type=ALL` (Full Table Scan 발생) |
| After | 개선 상태 | `name` 컬럼에 인덱스 추가 → `EXPLAIN type=ref` (인덱스 기반 조회) |

- **Before 증상**: 테이블 전체를 순차 탐색하므로 데이터가 많을수록 응답 시간이 선형 증가
- **After 효과**: 인덱스를 통해 해당 이름의 행을 빠르게 찾음

---

### 시나리오 2. 인덱스 무력화

**엔드포인트**: `GET /sql/orders/by-year?year=`

| 구분 | 상태 | 설명 |
|------|------|------|
| Before | 문제 있는 상태 | `YEAR(created_at) = ?` 함수로 감싸거나 `LIKE '%keyword'` 사용 → 인덱스 무효화 |
| After | 개선 상태 | 범위 조건으로 변경 (`created_at BETWEEN ? AND ?`) → `EXPLAIN type=range` |

- **Before 증상**: 함수나 앞부분 와일드카드로 인해 인덱스를 사용하지 못하고 Full Scan 발생
- **After 효과**: 범위 조건으로 인덱스를 그대로 활용하여 스캔 범위를 최소화

---

### 시나리오 3. N+1 문제

**엔드포인트**: `GET /sql/posts/with-comments`

| 구분 | 상태 | 설명 |
|------|------|------|
| Before | 문제 있는 상태 | `@OneToMany` Lazy Loading → Post 1개당 Comment SELECT 1번 → N+1 쿼리 발생 |
| After | 개선 상태 | Fetch Join 또는 `@EntityGraph` 사용 → 1번의 JOIN 쿼리로 해결 |

- **Before 증상**: Post가 100건이면 총 101번의 쿼리 실행 (1 + N)
- **After 효과**: 단일 JOIN 쿼리로 Post와 Comment를 함께 조회, 쿼리 횟수 대폭 감소

---

### 시나리오 4. 복합 인덱스 순서

**엔드포인트**: `GET /sql/logs/search`

| 구분 | 상태 | 설명 |
|------|------|------|
| Before | 문제 있는 상태 | 복합 인덱스 `(status, created_at)` 중 `created_at`만 조건으로 사용 → 인덱스 미사용 |
| After | 개선 상태 | 선행 컬럼(`status`) 포함하여 조회 → 복합 인덱스 활용 |

- **Before 증상**: 복합 인덱스는 선행 컬럼부터 순서대로 사용해야 하므로 `created_at`만 조건이면 인덱스 무시
- **After 효과**: `status + created_at` 조건으로 복합 인덱스를 온전히 활용

---

## API 엔드포인트 정의

| 메서드 | 경로 | 파라미터 | 설명 |
|--------|------|----------|------|
| GET | `/sql/users/search` | `name` (String) | name 컬럼 조회 — Full Table Scan vs 인덱스 조회 비교 |
| GET | `/sql/orders/by-year` | `year` (int) | 연도별 주문 조회 — 함수 감싸기로 인한 인덱스 무력화 비교 |
| GET | `/sql/posts/with-comments` | 없음 | Post + Comment 전체 조회 — N+1 문제 비교 |
| GET | `/sql/logs/search` | `status` (String), `from` (date), `to` (date) | 로그 조회 — 복합 인덱스 순서 비교 |

각 엔드포인트는 before/after 버전을 별도 경로로 제공한다.

- 예: `/sql/users/search/before`, `/sql/users/search/after`

---

## 구현 체크리스트

- [ ] `User`, `Order`, `Post`, `Comment`, `Log` 엔티티 설계
- [ ] 100만 건 시드 데이터 스크립트 작성 (`scripts/seed/`)
- [ ] 각 엔드포인트에서 `EXPLAIN ANALYZE` 결과를 응답에 함께 반환
- [ ] before 버전 / after 버전 컨트롤러 분리 (예: `/sql/users/search/before`, `/sql/users/search/after`)
- [ ] 인덱스 DDL 스크립트 작성 (`schema.sql`)

---

## 성공 지표 (분석 기준)

- EXPLAIN type 개선: `ALL` → `ref` 또는 `range` 이상으로 개선
- 실행 시간 비교 (before vs after)

### EXPLAIN type 분류표

| type 값 | 의미 | 수준 |
|---------|------|------|
| ALL | Full Table Scan | 최악 |
| index | Full Index Scan | 나쁨 |
| range | 범위 인덱스 스캔 | 보통 |
| ref | 비고유 인덱스 조회 | 좋음 |
| const | 고유 인덱스 1건 조회 | 최선 |

---

## 관련 문서 링크

- `./ERD.md` — 엔티티 관계 다이어그램 및 인덱스 전략
- `../DEVELOP_SETTING_GUIDE.md#sql-lab-설정` — 시드 데이터 및 EXPLAIN 실행 방법
- `../scenarios/README.md` — 분석 결과 기록 템플릿
- `../README.md` — 전체 학습 경로
