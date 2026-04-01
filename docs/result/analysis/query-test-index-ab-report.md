# 인덱스 단일화 A/B 테스트 보고서

## 1. 목적

현재 쿼리(`ST_DWithin + MBR + 리뷰순 코스`) 기준으로 `place` 공간 인덱스를 단일화할 수 있는지 확인했다.

비교 모드:

- `both`: `idx_place_location_gist` + `idx_place_location_geom_gist`
- `geography_only`: `idx_place_location_gist`만 유지
- `geometry_only`: `idx_place_location_geom_gist`만 유지

## 2. 측정 쿼리

- Place 현재 쿼리: 반경 1.5km + MBR + `ST_DWithin`
- Course 리뷰순 현재 쿼리: 주변 코스 추출 후 댓글 수 정렬 (`LATERAL COUNT`)
- 측정 방식: `EXPLAIN (ANALYZE, BUFFERS)` 단일 실행

원본 로그:

- `docs/result/raw/query-test-index-ab-10k-both-raw.txt`
- `docs/result/raw/query-test-index-ab-10k-geography-only-raw.txt`
- `docs/result/raw/query-test-index-ab-10k-geometry-only-raw.txt`
- `docs/result/raw/query-test-index-ab-100k-both-raw.txt`
- `docs/result/raw/query-test-index-ab-100k-geography-only-raw.txt`
- `docs/result/raw/query-test-index-ab-100k-geometry-only-raw.txt`

## 3. 결과 요약 (Execution Time, ms)

| 데이터셋 | 인덱스 모드 | Place | Course 리뷰순 |
| --- | --- | ---: | ---: |
| 10k | both | 9.360 | 0.737 |
| 10k | geography_only | 5.823 | 0.953 |
| 10k | geometry_only | 5.447 | 1.073 |
| 100k | both | 8.773 | 5.615 |
| 100k | geography_only | 7.249 | 4.644 |
| 100k | geometry_only | 8.448 | 4.549 |

## 4. 실행계획 포인트

- 세 모드 모두 `course_comment`는 `Index Only Scan using idx_course_comment_course_id`를 사용했다.
- Place 쿼리는 모드별로 서로 다른 단일/조합 인덱스를 타며 `Bitmap Heap Scan` 중심으로 실행됐다.
- 단일 실행 기준에서는 특정 모드가 항상 우세하지 않았다.

## 5. 결론

- 단일화는 지금 단계에서 **보류**한다.
- 이유:
  - 10k/100k, Place/Course 쿼리 조합에서 승자가 일관되지 않음
  - 쿼리 패턴 자체가 `geography` 연산(`ST_DWithin`) + `geometry` 연산(`MBR`)을 동시에 사용
- 운영 기본안:
  - `idx_place_location_gist` + `idx_place_location_geom_gist` **둘 다 유지**

## 6. 후속 권장

- 단일화 재검토는 1회성 측정이 아니라 동일 조건 10회 이상 반복 후 중앙값/95p로 판단한다.
