# 쿼리 최적화 결과 문서

## 1. 초기 검증 목표[^sec1]

1. GiST 적용 시 Baseline 대비 실행 시간이 유의미하게 개선되는지 확인
2. MBR(`&&`) 추가 시 GiST 단독 대비 추가 개선이 있는지 확인
3. 조회 범위(좁은/일반/넓은) 증가에 따른 스캔/조인 비용 변화를 확인
4. 데이터셋 확장(10k -> 100k) 시에도 최적화 전략이 유효한지 검증
5. 운영용 회귀 성능 기준선(시간 + 실행계획)을 고정

---

## 2. 검증 결과(목표 대비)[^sec2]

| 검증 목표 | 결과 |
| --- | --- |
| GiST 적용 효과 | 달성. Place/Course 모두 Baseline 대비 실행시간 대폭 감소 |
| MBR 추가 효과 | 달성. 대부분 시나리오에서 GiST 단독 대비 추가 개선 확인 |
| 범위 확대 비용 추적 | 달성. Baseline에서 `Seq Scan` 및 Join Filter 비용 급증 확인 |
| 10k -> 100k 확장성 | 달성. 최적화 케이스는 데이터 증가 후에도 실용 성능 유지 |
| 회귀 기준선 고정 | 달성. 10k/100k 기준선과 실행계획 가드레일 문서화 완료 |

---

## 3. 핵심 결과 요약[^sec3]

### 3.1 문제 구간(Baseline)

- `Seq Scan on place` 중심 실행
- `Nested Loop + Join Filter` 비용 급증
- 100k 넓은 범위 Course 조회에서 초 단위 지연 발생

### 3.2 최적화 적용 후

- `MBR &&`로 후보군 선축소 후 정밀 연산(`ST_DWithin`, `ST_Intersects`) 수행
- `Bitmap/Index Scan` 중심으로 실행계획 전환
- 리뷰순 쿼리는 `LATERAL COUNT` 구조와 댓글 인덱스로 집계 비용 절감

### 3.3 인덱스 전략

- 유지 인덱스:
`idx_place_location_gist`, `idx_place_location_geom_gist`, `idx_pin_course_id`, `idx_pin_course_place_id`, `idx_course_comment_course_id`
- 인덱스 단일화 A/B 결론:
우세 패턴이 일관되지 않아 단일화는 보류, 이중 인덱스 유지가 안전

---

## 4. 최종 결론[^sec4]

1. 본 최적화는 초기 검증 목표를 충족했다.
2. 핵심은 단순 인덱스 추가가 아니라, MBR 선필터를 포함한 연산 순서 최적화다.
3. 이후 변경은 회귀 기준선 문서의 시간/실행계획 가드레일 기준으로 검증한다.

---

## 5. 참고사항

### 5.1 문서 목차

- 보고서
- [최종 통합 보고서](./reports/query-test-comparison-report.md)
- [10k 결과 보고서](./reports/query-test-report-10k.md)
- [100k 결과 보고서](./reports/query-test-report-100k.md)
- [단계 완료 보고서](./reports/query-test-next-step-complete.md)

- 분석
- [인덱스 단일화 A/B 보고서](./analysis/query-test-index-ab-report.md)
- [회귀 성능 기준선](./analysis/query-test-regression-baseline.md)

- 실행계획
- [10k 실행계획 상세](./execution-report/query-test-execution-plans-10k.md)
- [100k 실행계획 상세](./execution-report/query-test-execution-plans-100k.md)

- 원본 로그
- `./raw/`

---

[^sec1]: 이 내용을 더 자세히 알고 싶다면 [query-test-plan.md](../plan/query-test-plan.md), [query-test-bootstrap.md](../plan/query-test-bootstrap.md)를 참고하세요.
[^sec2]: 이 내용을 더 자세히 알고 싶다면 [query-test-report-10k.md](./reports/query-test-report-10k.md), [query-test-report-100k.md](./reports/query-test-report-100k.md), [query-test-comparison-report.md](./reports/query-test-comparison-report.md)를 참고하세요.
[^sec3]: 이 내용을 더 자세히 알고 싶다면 [query-test-execution-plans-10k.md](./execution-report/query-test-execution-plans-10k.md), [query-test-execution-plans-100k.md](./execution-report/query-test-execution-plans-100k.md), `./raw/` 로그를 참고하세요.
[^sec4]: 이 내용을 더 자세히 알고 싶다면 [query-test-index-ab-report.md](./analysis/query-test-index-ab-report.md), [query-test-regression-baseline.md](./analysis/query-test-regression-baseline.md), [query-test-next-step-complete.md](./reports/query-test-next-step-complete.md)를 참고하세요.
