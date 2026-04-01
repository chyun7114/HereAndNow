# 쿼리 최적화 최종보고서 (100k, 시나리오 분기 기록)

## 1. 실행 개요

- 데이터셋: `100k` (`place=100,000`, `pin=100,000`, `course=28,000`)
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

- 동일 데이터셋(100k), 동일 컨테이너 환경
- 동일 공간 함수(`ST_Intersects`)
- 차이는 인덱스 존재 여부와 `&&`(MBR) 조건 추가 여부만 둠

---

## 3. 시나리오별 성능 결과

## 3.1 Place 실행시간 (ms)

| 시나리오 | Baseline | Case1 (GiST) | Case2 (GiST+MBR) |
| -------- | -------: | -----------: | ---------------: |
| 좁은     |  125.672 |        0.595 |            0.236 |
| 일반     |  109.830 |       22.207 |            2.027 |
| 넓은     |  111.402 |       30.647 |            4.232 |

해석:

- Baseline은 100k에서 모든 시나리오가 100ms 이상으로 높게 측정됐다.
- GiST만 적용한 Case1도 개선되지만, 일반/넓은에서는 병렬 Bitmap 경로 비용이 커서 10k 대비 절대시간이 눈에 띄게 증가했다.
- Case2(MBR 명시)는 세 시나리오 모두 Case1보다 빠르며, 특히 일반/넓은에서 개선폭이 크다.

## 3.2 Course 실행시간 (ms)

| 시나리오 | Baseline | Case1 (GiST) | Case2 (GiST+MBR) |
| -------- | -------: | -----------: | ---------------: |
| 좁은     |  560.320 |        2.377 |            1.831 |
| 일반     | 2639.141 |       27.465 |           15.210 |
| 넓은     | 7463.794 |       35.695 |           16.935 |

해석:

- Course Baseline은 범위 증가에 매우 민감하며, 넓은 범위에서 7초대를 기록했다.
- GiST 적용 후(케이스 1/2) 절대시간이 크게 줄었고, 100k에서도 최적화 효과가 유지됐다.
- 100k에서는 좁은/일반/넓은 모두 Case2가 Case1보다 유리하게 측정됐다.

---

## 4. 실행계획 분석 요약

### 4.1 Place 관점

- Baseline:
  - `Parallel Seq Scan on place` 중심
  - `Rows Removed by Filter`가 대량 발생
  - 병렬 스캔으로도 정밀 공간 필터 비용이 크게 남음
- Case1:
  - `Bitmap Index Scan + Bitmap Heap Scan` 경로
  - 일반/넓은은 `Parallel Bitmap Heap Scan`으로 전환
  - 인덱스 조건은 반영되지만 힙 재검증 범위가 커지면 비용 증가
- Case2:
  - `&&` 명시로 후보군을 더 강하게 제한
  - 좁은은 `Index Scan`, 일반/넓은은 `Bitmap` 경로
  - 재검증 비용이 줄어들어 Case1 대비 개선

### 4.2 Course 관점

- Baseline:
  - `Nested Loop Semi Join` + 대량 후보 결합
  - `Rows Removed by Join Filter`가 범위 증가와 함께 급증
  - 병렬 place 스캔이 들어가도 join 비용이 지배적
- Case1:
  - 시나리오에 따라 `HashAggregate`, `Parallel Hash Join`, `Hash Semi Join` 등으로 전환
  - Baseline 대비 대폭 개선되지만 넓은 범위에서는 여전히 비용 증가
- Case2:
  - `&&`를 명시해 place 후보를 1차 축소
  - 이후 `idx_pin_place_id` + `course_pkey` 경로를 안정적으로 사용
  - 100k에서는 전 시나리오에서 Case1보다 빠른 결과

### 4.3 종합 정리

- 100k에서도 병목의 본질은 `대량 후보군 + 비효율 조인`이다.
- GiST는 필수이며, 대용량일수록 MBR(`&&`)의 추가 가치가 더 커졌다.
- 운영 쿼리에서도 bbox 성격이 있는 조회라면 `&& + 정밀함수` 조합을 기본 전략으로 가져가는 것이 유리하다.

---

## 5. 쿼리 실행계획 문서

실행 쿼리와 케이스별 실행계획(간소화)은 별도 문서로 분리했다.

- 상세 문서: [query-test-execution-plans-100k.md](c:/Users/user/Desktop/HereAndNow/docs/result/execution-report/query-test-execution-plans-100k.md)

## 6. 단계별 결론

1. 100k에서도 GiST 인덱스 적용 효과는 모든 시나리오에서 명확하다.
2. 100k에서는 MBR(`&&`) 추가 효과가 전 시나리오에서 일관되게 나타났다.
3. Baseline은 데이터가 커질수록 조인 비용이 급격히 증가하므로 실운영에 부적합하다.
4. 다음 단계로는 동일 템플릿으로 10k/100k를 한 문서에서 교차 비교해 스케일 민감도를 정리하는 것이 좋다.
