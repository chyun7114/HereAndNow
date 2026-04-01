# 쿼리 최적화 단계 완료 보고서

## 1. 수행 항목

이번 단계에서 아래 3가지를 한 번에 완료했다.

1. 인덱스 단일화 A/B 테스트
2. 회귀 성능 테스트 기준선 고정
3. 결과 문서화(최종 정리)

## 2. 핵심 결론

- 인덱스 단일화는 보류한다.
- 현재 쿼리 패턴은 `geography(ST_DWithin)` + `geometry(MBR)`를 동시에 사용하므로 두 인덱스 유지가 안전하다.
- 회귀 기준선은 10k/100k 각각 고정했고, 시간/실행계획 가드레일까지 정의했다.

## 3. 산출물

### 3.1 A/B 보고서

- `docs/result/query-test-index-ab-report.md`

### 3.2 회귀 기준선

- `docs/result/query-test-regression-baseline.md`

### 3.3 원본 로그

- `docs/result/raw/query-test-index-ab-10k-both-raw.txt`
- `docs/result/raw/query-test-index-ab-10k-geography-only-raw.txt`
- `docs/result/raw/query-test-index-ab-10k-geometry-only-raw.txt`
- `docs/result/raw/query-test-index-ab-100k-both-raw.txt`
- `docs/result/raw/query-test-index-ab-100k-geography-only-raw.txt`
- `docs/result/raw/query-test-index-ab-100k-geometry-only-raw.txt`

## 4. 현재 운영 권장안

- 인덱스 유지:
  - `idx_place_location_gist`
  - `idx_place_location_geom_gist`
  - `idx_pin_course_id`
  - `idx_pin_course_place_id`
  - `idx_course_comment_course_id`
- 성능 검증은 회귀 기준선 문서의 절차/가드레일을 그대로 사용
