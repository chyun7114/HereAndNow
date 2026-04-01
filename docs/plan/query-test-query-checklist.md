# 쿼리 테스트 체크리스트 (query-test-plan 실행용)

## 목차

- 1. 문서 목적
- 2. 프로젝트에서 실제 사용 중인 공간 쿼리
- 3. viewport 성능 비교용 SQL (Baseline / GiST / GiST+MBR)
- 4. 실행 계획에서 반드시 확인할 항목
- 5. 사전 점검 SQL
- 6. 실행 순서 체크리스트

---

## 1. 문서 목적

`docs/plan/query-test-plan.md`를 실제로 수행할 때, 이 프로젝트에서 어떤 쿼리를 대상으로 성능을 확인해야 하는지 정리한 문서다.

주의사항:

- 현재 서비스 코드는 `viewport bbox` 직접 조회보다 `중심점 + 반경(ST_DWithin)` 조회를 주로 사용한다.
- 테스트 플랜의 `Baseline → GiST → GiST+MBR` 비교를 수행하려면, 아래 3장 SQL을 별도로 실행해 검증해야 한다.

---

## 2. 프로젝트에서 실제 사용 중인 공간 쿼리

### 2.1 장소 추천/광고 조회 (반경 1.5km)

- 위치: `src/main/java/com/meetup/hereandnow/place/infrastructure/repository/PlaceRepository.java`
- 메서드: `findPlacesByLocation`, `findNearbyPlaceIds`

```sql
SELECT *
FROM place p
WHERE ST_DWithin(
    p.location,
    ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
    1500
);
```

```sql
SELECT p.id
FROM place p
WHERE ST_DWithin(
    p.location,
    ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
    1500
);
```

### 2.2 코스 추천 조회 (반경 1.5km, 장소-핀-코스 조인)

- 위치: `src/main/java/com/meetup/hereandnow/course/infrastructure/repository/CourseRepository.java`
- 메서드: `findNearbyCourseIds`, `findNearbyCourseIdsSortedByCommentCount`

```sql
SELECT c.id
FROM course c
WHERE c.is_public = true
  AND EXISTS (
      SELECT 1
      FROM pin p
      JOIN place pl ON p.place_id = pl.id
      WHERE p.course_id = c.id
        AND ST_DWithin(pl.location, :point, 1500)
  );
```

```sql
SELECT c.id
FROM course c
LEFT JOIN course_comment cc ON cc.course_id = c.id
WHERE c.is_public = true
  AND EXISTS (
      SELECT 1
      FROM pin p
      JOIN place pl ON p.place_id = pl.id
      WHERE p.course_id = c.id
        AND ST_DWithin(pl.location, :point, 1500)
  )
GROUP BY c.id
ORDER BY COUNT(cc.id) DESC, c.id DESC;
```

### 2.3 기본 인덱스 확인 대상

- 위치: `src/main/resources/db/migration/V1__init_schema.sql`
- 인덱스: `idx_place_location_gist` (`place.location` geography GiST)

---

## 3. viewport 성능 비교용 SQL (Baseline / GiST / GiST+MBR)

`query-test-plan.md`의 viewport 테스트를 맞추기 위한 직접 실행용 SQL이다.

### 3.1 사전 준비 (중요)

`place.location` 컬럼 타입은 `geography(Point, 4326)` 이므로, `location::geometry` 기반 bbox 연산을 테스트하려면 표현식 인덱스가 필요하다.

```sql
CREATE INDEX IF NOT EXISTS idx_place_location_geom_gist
    ON place
    USING GIST ((location::geometry));
```

파라미터:

- `:minLon`, `:minLat`, `:maxLon`, `:maxLat`

공통 bbox:

```sql
ST_MakeEnvelope(:minLon, :minLat, :maxLon, :maxLat, 4326)
```

### 3.2 Place 조회 비교 쿼리

#### Baseline (인덱스/MBR 없이 정밀 연산만)

```sql
SELECT p.id, p.place_name, p.location
FROM place p
WHERE ST_Intersects(
    p.location::geometry,
    ST_MakeEnvelope(:minLon, :minLat, :maxLon, :maxLat, 4326)
);
```

#### Case 1 (GiST만 적용)

Baseline과 SQL 형태는 동일하고, `idx_place_location_geom_gist` 인덱스가 존재하는 상태로 실행한다.

```sql
SELECT p.id, p.place_name, p.location
FROM place p
WHERE ST_Intersects(
    p.location::geometry,
    ST_MakeEnvelope(:minLon, :minLat, :maxLon, :maxLat, 4326)
);
```

#### Case 2 (GiST + MBR)

```sql
SELECT p.id, p.place_name, p.location
FROM place p
WHERE p.location::geometry && ST_MakeEnvelope(:minLon, :minLat, :maxLon, :maxLat, 4326)
  AND ST_Intersects(
      p.location::geometry,
      ST_MakeEnvelope(:minLon, :minLat, :maxLon, :maxLat, 4326)
  );
```

### 3.3 Course 조회 비교 쿼리 (EXISTS 내부 공간 조건)

#### Baseline

```sql
SELECT c.id
FROM course c
WHERE c.is_public = true
  AND EXISTS (
      SELECT 1
      FROM pin p
      JOIN place pl ON p.place_id = pl.id
      WHERE p.course_id = c.id
        AND ST_Intersects(
            pl.location::geometry,
            ST_MakeEnvelope(:minLon, :minLat, :maxLon, :maxLat, 4326)
        )
  );
```

#### Case 1 (GiST만 적용)

```sql
SELECT c.id
FROM course c
WHERE c.is_public = true
  AND EXISTS (
      SELECT 1
      FROM pin p
      JOIN place pl ON p.place_id = pl.id
      WHERE p.course_id = c.id
        AND ST_Intersects(
            pl.location::geometry,
            ST_MakeEnvelope(:minLon, :minLat, :maxLon, :maxLat, 4326)
        )
  );
```

#### Case 2 (GiST + MBR)

```sql
SELECT c.id
FROM course c
WHERE c.is_public = true
  AND EXISTS (
      SELECT 1
      FROM pin p
      JOIN place pl ON p.place_id = pl.id
      WHERE p.course_id = c.id
        AND pl.location::geometry && ST_MakeEnvelope(:minLon, :minLat, :maxLon, :maxLat, 4326)
        AND ST_Intersects(
            pl.location::geometry,
            ST_MakeEnvelope(:minLon, :minLat, :maxLon, :maxLat, 4326)
        )
  );
```

---

## 4. 실행 계획에서 반드시 확인할 항목

각 케이스마다 아래를 `EXPLAIN (ANALYZE, BUFFERS, VERBOSE)`로 확인한다.

- Baseline:
  - `Seq Scan on place` 비중이 높은지 확인
  - `Rows Removed by Filter`가 큰지 확인
- Case 1 (GiST):
  - `Index Scan` 또는 `Bitmap Index Scan`이 등장하는지 확인
  - `idx_place_location_geom_gist` 사용 여부 확인
- Case 2 (GiST + MBR):
  - `Index Cond`에 `&&` 조건이 반영되는지 확인
  - `Filter`에서 `ST_Intersects` 정밀 연산 건수가 줄었는지 확인
- 공통:
  - `Execution Time`, `Buffers`, `actual rows` 비교
  - 조회 결과 row 수가 케이스 간 동일한지 확인

---

## 5. 사전 점검 SQL

### 5.1 PostGIS/인덱스 상태

```sql
SELECT postgis_full_version();
```

```sql
SELECT schemaname, tablename, indexname, indexdef
FROM pg_indexes
WHERE tablename = 'place'
ORDER BY indexname;
```

### 5.2 통계 최신화

```sql
ANALYZE place;
ANALYZE pin;
ANALYZE course;
ANALYZE course_comment;
```

### 5.3 성능 측정 실행 템플릿

```sql
EXPLAIN (ANALYZE, BUFFERS, VERBOSE)
SELECT p.id
FROM place p
WHERE p.location::geometry && ST_MakeEnvelope(:minLon, :minLat, :maxLon, :maxLat, 4326)
  AND ST_Intersects(
      p.location::geometry,
      ST_MakeEnvelope(:minLon, :minLat, :maxLon, :maxLat, 4326)
  );
```

---

## 6. 실행 순서 체크리스트

1. 데이터셋(10k / 100k) 준비
2. `ANALYZE` 수행
3. Baseline 쿼리 30회 반복 (워밍업 1~2회 제외)
4. Case 1 쿼리 30회 반복
5. Case 2 쿼리 30회 반복
6. 각 케이스별 `EXPLAIN (ANALYZE, BUFFERS)` 저장
7. 평균 / p95 / 최대 실행시간 비교
8. 실행 계획에서 인덱스 사용 및 `&&` 반영 여부 확인
9. Place 결과 row 수와 Course 결과 row 수가 케이스 간 동일한지 검증

테스트 결과 기록 시 최소 항목:

- 케이스명 (Baseline / Case1 / Case2)
- 데이터셋 크기
- bbox 범위 (좁은/일반/넓은 + 실제 좌표)
- 평균/ms, p95/ms, 최대/ms
- 대표 실행 계획 요약 (Scan 타입, Index 이름, Buffers)
