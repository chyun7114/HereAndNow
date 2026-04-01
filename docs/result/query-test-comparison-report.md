# 쿼리 최적화 통합 비교 보고서 (10k vs 100k)

## 1. 비교 목적

본 문서는 동일한 테스트 시나리오(좁은/일반/넓은)에서
`10k`와 `100k` 데이터셋의 성능 결과를 교차 비교해,
스케일 증가 시 최적화 전략(GiST, MBR)의 유효성을 정리한다.

---

## 2. 비교 기준

- 공통 DB 환경: PostgreSQL 16 + PostGIS 3.4.3
- 공통 함수: `ST_Intersects`
- 공통 비교군:
  - Baseline: 인덱스/MBR 미적용
  - Case1: GiST 적용
  - Case2: GiST + MBR(`&&`) 적용
- 공통 시나리오:
  - 좁은: 약 `2km x 2km`
  - 일반: 약 `5km x 5km`
  - 넓은: 약 `9km x 9km`

---

## 3. Place 통합 비교 (ms)

| 시나리오 | 10k Baseline | 10k Case1 | 10k Case2 | 100k Baseline | 100k Case1 | 100k Case2 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 좁은 | 129.937 | 0.254 | 0.137 | 125.672 | 0.595 | 0.236 |
| 일반 | 16.983 | 0.401 | 0.239 | 109.830 | 22.207 | 2.027 |
| 넓은 | 27.117 | 0.961 | 0.475 | 111.402 | 30.647 | 4.232 |

핵심 해석:

- Baseline은 데이터가 커지면 일반/넓은에서 급격히 악화된다.
- 100k에서 `Case1`은 개선되지만 일반/넓은에서 절대 시간이 여전히 커진다.
- `Case2`는 10k/100k 모두 일관되게 가장 안정적이며, 100k에서 특히 효과가 크다.

---

## 4. Course 통합 비교 (ms)

| 시나리오 | 10k Baseline | 10k Case1 | 10k Case2 | 100k Baseline | 100k Case1 | 100k Case2 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 좁은 | 41.299 | 0.291 | 0.171 | 560.320 | 2.377 | 1.831 |
| 일반 | 199.219 | 1.696 | 0.722 | 2639.141 | 27.465 | 15.210 |
| 넓은 | 618.473 | 2.279 | 2.795 | 7463.794 | 35.695 | 16.935 |

핵심 해석:

- Baseline은 스케일 증가에 매우 취약하며, 100k 넓은 범위에서 7초대까지 증가한다.
- Case1/Case2 모두 대규모 개선을 제공한다.
- 10k 넓은 범위에서는 Case1이 Case2보다 빠른 예외가 있었지만,
  100k에서는 전 범위에서 Case2가 더 빠르게 측정됐다.

---

## 5. 실행계획 관점 통합 해석

1. Baseline 공통 병목:
   - `Seq Scan` 중심 + 대량 필터링 + 비효율 조인
   - `Rows Removed by Filter/Join Filter` 급증
2. Case1 공통 변화:
   - GiST 인덱스 활용으로 후보군 축소
   - 조인 경로가 해시/인덱스 기반으로 개선
3. Case2 공통 변화:
   - `&&`로 후보군 선축소 후 정밀 판정
   - 대용량(100k)에서 재검증 비용 절감 효과가 더 명확

---

## 6. 운영 적용 가이드

1. 기본 전략:
   - 공간 조회는 `GiST`를 기본 전제로 사용
2. 권장 쿼리 패턴:
   - `MBR(&&)` + 정밀 함수(`ST_Intersects`) 조합을 기본 채택
3. 예외 고려:
   - 데이터가 작고 범위가 제한적인 일부 구간에서는 Case1 우세 가능
   - 그러나 확장성/일관성 관점에서는 Case2가 더 안전
4. 검증 운영화:
   - 신규 인덱스/쿼리 변경 시 10k/100k 동일 시나리오 회귀 측정 권장

---

## 7. 원본 보고서 링크

- 10k 메인 보고서:
  - [query-test-final-report-10k.md](c:/Users/user/Desktop/HereAndNow/docs/result/query-test-final-report-10k.md)
- 10k 실행계획 상세:
  - [query-test-execution-plans-10k.md](c:/Users/user/Desktop/HereAndNow/docs/result/execution-report/query-test-execution-plans-10k.md)
- 100k 메인 보고서:
  - [query-test-final-report-100k.md](c:/Users/user/Desktop/HereAndNow/docs/result/query-test-final-report-100k.md)
- 100k 실행계획 상세:
  - [query-test-execution-plans-100k.md](c:/Users/user/Desktop/HereAndNow/docs/result/execution-report/query-test-execution-plans-100k.md)

