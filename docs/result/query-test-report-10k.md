# 쿼리 최적화 최종보고서 (시나리오 분기 기록)

## 1. 실행 개요

- 데이터셋: `10k` (`place=10,000`, `pin=10,000`, `course=2,800`)
- DB: PostgreSQL 16 + PostGIS 3.4.3
- 측정 방식: `EXPLAIN (ANALYZE, BUFFERS)`
- 비교군:
  - Baseline: 인덱스/MBR 미적용
  - Case1: GiST 적용
  - Case2: GiST + MBR(`&&`) 적용

---

## 2. 시나리오 분기 기준 (좁은/일반/넓은)

이번 검증은 `query-test-plan.md`의 시나리오 A/B/C(좁은/일반/넓은 범위)를
실제 bbox로 고정하여 동일 조건 반복 측정하는 방식으로 진행했다.

- 기준 중심점:
  - 강남역 근처 (`lon=127.026842105662`, `lat=37.4982667167977`)
- 분기 목적:
  - 좁은: 후보군이 적은 상황에서 인덱스/MBR 효과 확인
  - 일반: 실제 사용자 사용 패턴에 가까운 중간 범위 검증
  - 넓은: 후보군이 커질 때 플래너의 조인/스캔 전략 변화 확인

시나리오별 bbox:

- 좁은:
  - `minLon=127.0154`, `minLat=37.4893`, `maxLon=127.0382`, `maxLat=37.5073`
  - 대략 `2km x 2km`
- 일반:
  - `minLon=126.9983`, `minLat=37.4758`, `maxLon=127.0553`, `maxLat=37.5208`
  - 대략 `5km x 5km`
- 넓은:
  - `minLon=126.9755`, `minLat=37.4578`, `maxLon=127.0781`, `maxLat=37.5388`
  - 대략 `9km x 9km`

통제 조건:

- 동일 데이터셋(10k), 동일 컨테이너 환경
- 동일 공간 함수(`ST_Intersects`)
- 차이는 인덱스 존재 여부와 `&&`(MBR) 조건 추가 여부만 둠

---

## 3. 시나리오별 성능 결과

## 3.1 Place 실행시간 (ms)

| 시나리오 | Baseline | Case1 (GiST) | Case2 (GiST+MBR) |
| -------- | -------: | -----------: | ---------------: |
| 좁은     |  129.937 |        0.254 |            0.137 |
| 일반     |   16.983 |        0.401 |            0.239 |
| 넓은     |   27.117 |        0.961 |            0.475 |

해석:

- 세 시나리오 모두 GiST 적용 시 Baseline 대비 큰 폭으로 개선됐다.
- Place 조회는 모든 시나리오에서 `Case2`가 `Case1`보다 빠르게 측정됐다.
- 좁은 범위에서 Baseline의 상대적 비효율이 가장 크게 드러났다.

## 3.2 Course 실행시간 (ms)

| 시나리오 | Baseline | Case1 (GiST) | Case2 (GiST+MBR) |
| -------- | -------: | -----------: | ---------------: |
| 좁은     |   41.299 |        0.291 |            0.171 |
| 일반     |  199.219 |        1.696 |            0.722 |
| 넓은     |  618.473 |        2.279 |            2.795 |

해석:

- Course 조회는 Baseline에서 범위가 넓어질수록 급격히 느려졌다.
- GiST 적용 후(케이스 1/2)에는 절대 실행시간이 매우 작은 수준으로 감소했다.
- 넓은 범위에서는 `Case2`가 `Case1`보다 소폭 느렸고,
  이는 플래너의 조인 경로 선택 차이(해시 결합 vs 중첩 루프/집계 경로) 영향으로 해석된다.

---

## 4. 실행계획 분석 요약

### 4.1 Place 관점

- Baseline:
  - 공통적으로 `Seq Scan on place`
  - `Rows Removed by Filter`가 크게 발생
  - 정밀 공간 연산(`ST_Intersects`)을 넓은 후보군에 직접 적용
- Case1:
  - `Bitmap Index Scan on idx_place_location_geom_gist`
  - `Bitmap Heap Scan`으로 힙 재검증
  - `Index Cond: location::geometry && envelope` 반영
- Case2:
  - `&&`를 명시해 MBR 후보군 축소 의도를 쿼리 레벨에서 고정
  - 좁은 범위에서는 `Index Scan` 단일 경로로 전환
  - 일반/넓은 범위는 `Bitmap` 경로 유지 + 실행시간 개선

### 4.2 Course 관점

- Baseline:
  - 상위 `Nested Loop Semi Join`
  - 내부 `place Seq Scan + pin Seq Scan` 조합
  - `Rows Removed by Join Filter`가 범위 증가와 함께 폭증
- Case1:
  - `Hash Join` 또는 인덱스 기반 `Nested Loop`로 전환
  - place 접근이 GiST 기반 후보군 축소로 변경
- Case2:
  - `&&`로 후보군 선축소 후 `ST_Intersects` 정밀판정
  - `idx_pin_place_id` 활용 경로가 안정적으로 관찰
  - 넓은 범위에서는 플래너 경로 선택에 따라 Case1 우세 가능

### 4.3 종합 정리

- Baseline의 병목은 `Seq Scan + 대량 필터링 + 비효율 조인`이다.
- GiST 적용만으로도 대부분 병목이 해소된다.
- MBR(`&&`)은 보통 추가 이득을 주지만,
  항상 절대 우위는 아니며 데이터 분포/범위/플래너 선택에 따라 달라질 수 있다.

---

## 5. 쿼리 실행계획 문서

실행 쿼리와 케이스별 실행계획(간소화)은 별도 문서로 분리했다.

- 상세 문서: [query-test-execution-plans.md](./execution-report/query-test-execution-plans-10k.md)

## 6. 단계별 결론

1. GiST 인덱스 적용 효과는 3개 시나리오 모두에서 명확하다.
2. MBR(`&&`) 추가 효과는 좁은/일반에서 확실히 유효하다.
3. 넓은 범위에서는 Case1이 더 유리할 수 있으므로, 운영 쿼리는 범위 조건별 분기 전략 검토가 필요하다.
4. 다음 단계(100k)에서도 동일 시나리오 분기로 재측정해 범위/데이터 크기 교차 비교를 해야 한다.
