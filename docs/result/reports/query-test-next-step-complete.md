# 쿼리 최적화 최종 보고서

## 1. 최종 결론

1. 공간 조회 쿼리는 `GiST + MBR` 조합 적용으로 스캔 범위를 줄이고 실행 시간을 안정적으로 단축했다.
2. 인덱스 단일화 A/B 결과는 우세 모드가 일관되지 않아, 현재는 `geography`/`geometry` 인덱스를 모두 유지하는 것이 안전하다.
3. 10k/100k 기준 회귀 성능 기준선과 실행계획 가드레일을 고정해, 이후 변경 시 성능 퇴행 여부를 즉시 판단할 수 있도록 정리했다.

## 2. 최종 산출물

1. 인덱스 단일화 A/B 보고서: [query-test-index-ab-report.md](../analysis/query-test-index-ab-report.md)
2. 회귀 성능 기준선: [query-test-regression-baseline.md](../analysis/query-test-regression-baseline.md)
3. 단계 완료 보고서(본 문서): [query-test-next-step-complete.md](./query-test-next-step-complete.md)

## 3. 운영 적용안

1. 유지 인덱스
`idx_place_location_gist`, `idx_place_location_geom_gist`, `idx_pin_course_id`, `idx_pin_course_place_id`, `idx_course_comment_course_id`
2. 검증 방식
회귀 검증은 [query-test-regression-baseline.md](../analysis/query-test-regression-baseline.md)의 절차와 가드레일을 기본 규칙으로 적용한다.

## 4. 부록

원본 실행계획 로그는 `docs/result/raw/` 경로에 보관한다.


