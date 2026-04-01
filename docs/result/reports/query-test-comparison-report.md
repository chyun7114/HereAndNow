# 쿼리 최적화 최종 통합 보고서

## 1. 문서 목차(README 구조 연계)

### 1.1 보고서

1. [10k 결과 보고서](./query-test-report-10k.md)
2. [100k 결과 보고서](./query-test-report-100k.md)
3. [단계 완료 보고서](./query-test-next-step-complete.md)

### 1.2 분석

1. [인덱스 단일화 A/B 보고서](../analysis/query-test-index-ab-report.md)
2. [회귀 성능 기준선](../analysis/query-test-regression-baseline.md)

### 1.3 실행계획

1. [10k 실행계획 상세](../execution-report/query-test-execution-plans-10k.md)
2. [100k 실행계획 상세](../execution-report/query-test-execution-plans-100k.md)

### 1.4 원본 로그

1. `../raw/`

---

## 2. 초기 검증 목표

1. GiST 적용으로 Baseline 대비 실행 시간을 유의미하게 단축한다.
2. MBR(`&&`) 추가로 GiST 단독 대비 추가 개선 효과를 확인한다.
3. 조회 범위(좁은/일반/넓은) 확대 시 스캔/조인 비용 변화를 추적한다.
4. 데이터셋 확장(10k -> 100k)에서도 최적화 전략의 유효성을 확인한다.
5. 운영 기준으로 사용할 회귀 성능 기준선(시간/플랜)을 고정한다.

---

## 3. 검증 결과(목표 대비)

| 초기 검증 목표 | 검증 결과 |
| --- | --- |
| GiST 적용 효과 확인 | 달성. Place/Course 모두 Baseline 대비 실행 시간 대폭 감소 |
| MBR 추가 효과 확인 | 달성. 대부분 시나리오에서 GiST 단독 대비 추가 개선 확인 |
| 범위 확대 비용 추적 | 달성. Baseline에서 `Seq Scan` 및 Join Filter 비용 급증 패턴 확인 |
| 10k -> 100k 확장성 | 달성. 최적화 케이스는 데이터 증가에도 실용 성능 유지 |
| 회귀 기준선 고정 | 달성. 10k/100k 기준선 및 가드레일 문서화 완료 |

---

## 4. 최종 정리

1. 쿼리 구조
- 공간 조회는 `MBR &&`로 후보를 먼저 줄이고 `ST_DWithin`/`ST_Intersects`로 정밀 필터링한다.
- 리뷰순 코스 조회는 댓글 집계를 `LATERAL COUNT` 기반으로 변경해 인덱스 활용 경로를 확보했다.

2. 인덱스 전략
- `idx_place_location_gist`와 `idx_place_location_geom_gist`를 함께 유지한다.
- `idx_pin_course_id`, `idx_pin_course_place_id`, `idx_course_comment_course_id`를 유지한다.

3. 실행계획 관찰 요약
- 비최적화 구간: `Seq Scan` + 대량 Join Filter 비용이 지배적
- 최적화 구간: `Bitmap/Index Scan` 중심 전환, 댓글 집계는 `Index Only Scan` 유지

---

## 5. 최종 결론

1. 본 최적화는 초기 검증 목표를 충족했다.
2. 인덱스 단일화는 우세 패턴이 일관되지 않아 보류하고, 이중 인덱스 유지가 안전하다.
3. 이후 변경은 회귀 기준선 문서의 시간 상한과 실행계획 가드레일로 검증한다.
