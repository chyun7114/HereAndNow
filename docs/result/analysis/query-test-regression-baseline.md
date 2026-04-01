# 쿼리 회귀 성능 기준선 (고정)

## 1. 기준선 목적

쿼리/인덱스 변경 시 성능 회귀를 빠르게 판단하기 위한 고정 기준선이다.

기준 쿼리:

- Place 현재 반경 조회 쿼리
- Course 리뷰순 조회 쿼리

기준 인덱스 상태:

- `idx_place_location_gist` 유지
- `idx_place_location_geom_gist` 유지
- `idx_pin_course_id`, `idx_pin_course_place_id`, `idx_course_comment_course_id` 유지

## 2. 기준선 수치 (Execution Time, ms)

| 데이터셋 | Place 기준선 | Course 리뷰순 기준선 |
| --- | ---: | ---: |
| 10k | 9.360 | 0.737 |
| 100k | 8.773 | 5.615 |

원본 로그:

- `docs/result/raw/query-test-index-ab-10k-both-raw.txt`
- `docs/result/raw/query-test-index-ab-100k-both-raw.txt`

## 3. 회귀 판정 가드레일

### 3.1 시간 가드레일

| 데이터셋 | Place 상한 | Course 리뷰순 상한 |
| --- | ---: | ---: |
| 10k | 15 ms | 2 ms |
| 100k | 15 ms | 10 ms |

판정 규칙:

- 상한 초과 시 회귀 의심
- 2회 연속 초과 시 회귀 확정

### 3.2 실행계획 가드레일

- Place 쿼리에서 아래 둘 중 하나 이상 사용되어야 함
  - `idx_place_location_gist`
  - `idx_place_location_geom_gist`
- Course 리뷰순 쿼리에서 `course_comment`는 `Index Only Scan using idx_course_comment_course_id` 유지
- 아래 패턴이 등장하면 회귀 의심
  - `Seq Scan on course_comment`
  - Place 구간 전체 `Seq Scan on place`로 후퇴

## 4. 실행 절차 (고정)

1. `setup-query-test.ps1 -Dataset small` 실행
2. 현재 쿼리 `EXPLAIN (ANALYZE, BUFFERS)` 수집
3. 10k 기준선과 비교
4. `setup-query-test.ps1 -Dataset large` 실행
5. 현재 쿼리 `EXPLAIN (ANALYZE, BUFFERS)` 수집
6. 100k 기준선과 비교

## 5. 비고

- 하드웨어/도커 환경이 바뀌면 기준선 재보정이 필요하다.
