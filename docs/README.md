# Performance Lab — 학습 경로 인덱스

Kotlin/Spring 기반 백엔드 성능 문제를 직접 재현하고 디버깅하는 학습용 샌드박스입니다.
각 Lab은 **before(문제 재현) → after(개선)** 구조로 구성되며, 실제 증상을 눈으로 확인하고 분석하는 것을 목표로 합니다.

---

## Lab 학습 순서

아래 순서로 진행하는 것을 권장합니다. 각 Lab PRD 링크를 클릭해 상세 시나리오를 확인하세요.

| 순서 | Lab | 핵심 주제 | PRD |
|------|-----|----------|-----|
| 1순위 | SQL Execution Plan Lab | 실행계획 분석, 인덱스 최적화, N+1 해결 | [SQL Lab PRD](../docs/prd/sql-lab-prd.md) |
| 2순위 | Thread Pool Lab | Thread/Connection Pool 고갈, Actuator 모니터링 | [Thread Lab PRD](../docs/prd/thread-lab-prd.md) |
| 3순위 | Redis Cache Miss Lab | Cache Miss 원인 분석, Stampede 패턴 해결 | [Cache Lab PRD](../docs/prd/cache-lab-prd.md) |
| 4순위 | GC Lab | GC 로그 분석, 메모리 누수 재현 | [GC Lab PRD](../docs/prd/gc-lab-prd.md) |

---

## 공통 학습 워크플로우

모든 Lab은 동일한 5단계 흐름으로 학습합니다.

```
1단계. before 엔드포인트 호출
       └─ 문제가 내재된 코드로 요청을 보내 증상을 유발합니다.

2단계. 증상 확인
       └─ 응답 지연, 에러 발생, 메트릭 이상 등 실제 증상을 관찰합니다.

3단계. 원인 분석
       └─ EXPLAIN ANALYZE / Actuator 메트릭 / redis-cli / GC 로그 등 도구로 원인을 파악합니다.

4단계. after 코드 교체
       └─ 개선된 코드 또는 설정으로 교체합니다.

5단계. before/after 비교
       └─ 동일한 부하로 재측정하고 응답시간, 에러율, TPS 등을 비교·기록합니다.
```

---

## 문서 링크

| 문서 | 설명 |
|------|------|
| [환경 설정 가이드](./DEVELOP_SETTING_GUIDE.md) | Docker, JDK, k6, Redis CLI 등 개발 환경 구성 방법 |
| [시나리오 분석 노트](./scenarios/README.md) | before/after 분석 결과 기록 템플릿 및 작성 규칙 |
