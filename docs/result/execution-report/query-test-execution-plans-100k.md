# 쿼리 실행계획 상세 로그 (100k)

본 문서는 `query-test-final-report-100k.md`에서 분리한 실행 쿼리/간소 실행계획 상세 기록이다.

## 1. 좁은 시나리오

### Place - Baseline

```sql
-- 실행 쿼리
SELECT count(*)
FROM place p
WHERE ST_Intersects(
    p.location::geometry,
    ST_MakeEnvelope(127.0154, 37.4893, 127.0382, 37.5073, 4326)
);

-- 실행계획 분석 결과(간소화)
-- 주요 노드: Finalize Aggregate -> Gather -> Partial Aggregate -> Parallel Seq Scan on place
-- Rows Removed by Filter: 33,246 (worker당)
-- 병렬 스캔으로도 정밀 연산 비용이 큼
-- Execution Time: 125.672 ms
```

### Place - Case1 (GiST)

```sql
-- 실행 쿼리
SELECT count(*)
FROM place p
WHERE ST_Intersects(
    p.location::geometry,
    ST_MakeEnvelope(127.0154, 37.4893, 127.0382, 37.5073, 4326)
);

-- 실행계획 분석 결과(간소화)
-- 주요 노드: Aggregate -> Bitmap Heap Scan -> Bitmap Index Scan(idx_place_location_geom_gist)
-- Index Cond: location::geometry && envelope
-- 후보군: 261 rows
-- Execution Time: 0.595 ms
```

### Place - Case2 (GiST+MBR)

```sql
-- 실행 쿼리
SELECT count(*)
FROM place p
WHERE p.location::geometry && ST_MakeEnvelope(127.0154, 37.4893, 127.0382, 37.5073, 4326)
  AND ST_Intersects(
      p.location::geometry,
      ST_MakeEnvelope(127.0154, 37.4893, 127.0382, 37.5073, 4326)
  );

-- 실행계획 분석 결과(간소화)
-- 주요 노드: Aggregate -> Index Scan(idx_place_location_geom_gist)
-- Index Cond: (location::geometry && envelope) AND (location::geometry && envelope)
-- 후보군: 261 rows
-- Execution Time: 0.236 ms
```

### Course - Baseline

```sql
-- 실행 쿼리
SELECT count(*)
FROM course c
WHERE c.is_public = true
  AND EXISTS (
      SELECT 1
      FROM pin p
      JOIN place pl ON p.place_id = pl.id
      WHERE p.course_id = c.id
        AND ST_Intersects(
            pl.location::geometry,
            ST_MakeEnvelope(127.0154, 37.4893, 127.0382, 37.5073, 4326)
        )
  );

-- 실행계획 분석 결과(간소화)
-- 주요 노드: Aggregate -> Nested Loop Semi Join
-- 내부: course Seq Scan + place Parallel Seq Scan + pin Index Scan(idx_pin_place_id)
-- Rows Removed by Join Filter: 7,273,882
-- Execution Time: 560.320 ms
```

### Course - Case1 (GiST)

```sql
-- 실행 쿼리
SELECT count(*)
FROM course c
WHERE c.is_public = true
  AND EXISTS (
      SELECT 1
      FROM pin p
      JOIN place pl ON p.place_id = pl.id
      WHERE p.course_id = c.id
        AND ST_Intersects(
            pl.location::geometry,
            ST_MakeEnvelope(127.0154, 37.4893, 127.0382, 37.5073, 4326)
        )
  );

-- 실행계획 분석 결과(간소화)
-- 주요 노드: Aggregate -> Nested Loop
-- 내부: HashAggregate -> Bitmap Heap/Index Scan(place) -> Index Scan(pin idx_pin_place_id) -> Index Scan(course_pkey)
-- Execution Time: 2.377 ms
```

### Course - Case2 (GiST+MBR)

```sql
-- 실행 쿼리
SELECT count(*)
FROM course c
WHERE c.is_public = true
  AND EXISTS (
      SELECT 1
      FROM pin p
      JOIN place pl ON p.place_id = pl.id
      WHERE p.course_id = c.id
        AND pl.location::geometry && ST_MakeEnvelope(127.0154, 37.4893, 127.0382, 37.5073, 4326)
        AND ST_Intersects(
            pl.location::geometry,
            ST_MakeEnvelope(127.0154, 37.4893, 127.0382, 37.5073, 4326)
        )
  );

-- 실행계획 분석 결과(간소화)
-- 주요 노드: Aggregate -> Nested Loop
-- 내부: HashAggregate -> Index Scan(place idx_place_location_geom_gist) -> Index Scan(pin idx_pin_place_id) -> Index Scan(course_pkey)
-- Execution Time: 1.831 ms
```

## 2. 일반 시나리오

### Place - Baseline

```sql
-- 실행 쿼리
SELECT count(*)
FROM place p
WHERE ST_Intersects(
    p.location::geometry,
    ST_MakeEnvelope(126.9983, 37.4758, 127.0553, 37.5208, 4326)
);

-- 실행계획 분석 결과(간소화)
-- 주요 노드: Finalize Aggregate -> Gather -> Partial Aggregate -> Parallel Seq Scan on place
-- Rows Removed by Filter: 32,786 (worker당)
-- Execution Time: 109.830 ms
```

### Place - Case1 (GiST)

```sql
-- 실행 쿼리
SELECT count(*)
FROM place p
WHERE ST_Intersects(
    p.location::geometry,
    ST_MakeEnvelope(126.9983, 37.4758, 127.0553, 37.5208, 4326)
);

-- 실행계획 분석 결과(간소화)
-- 주요 노드: Finalize Aggregate -> Gather -> Partial Aggregate -> Parallel Bitmap Heap Scan
-- 인덱스 노드: Bitmap Index Scan(idx_place_location_geom_gist)
-- Index Cond: location::geometry && envelope
-- Execution Time: 22.207 ms
```

### Place - Case2 (GiST+MBR)

```sql
-- 실행 쿼리
SELECT count(*)
FROM place p
WHERE p.location::geometry && ST_MakeEnvelope(126.9983, 37.4758, 127.0553, 37.5208, 4326)
  AND ST_Intersects(
      p.location::geometry,
      ST_MakeEnvelope(126.9983, 37.4758, 127.0553, 37.5208, 4326)
  );

-- 실행계획 분석 결과(간소화)
-- 주요 노드: Aggregate -> Bitmap Heap Scan -> Bitmap Index Scan(idx_place_location_geom_gist)
-- Recheck Cond 포함, 후보군 1,643 rows
-- Execution Time: 2.027 ms
```

### Course - Baseline

```sql
-- 실행 쿼리
SELECT count(*)
FROM course c
WHERE c.is_public = true
  AND EXISTS (
      SELECT 1
      FROM pin p
      JOIN place pl ON p.place_id = pl.id
      WHERE p.course_id = c.id
        AND ST_Intersects(
            pl.location::geometry,
            ST_MakeEnvelope(126.9983, 37.4758, 127.0553, 37.5208, 4326)
        )
  );

-- 실행계획 분석 결과(간소화)
-- 주요 노드: Aggregate -> Nested Loop Semi Join
-- 내부: course Seq Scan + place Parallel Seq Scan + pin Index Scan(idx_pin_place_id)
-- Rows Removed by Join Filter: 44,688,341
-- Execution Time: 2,639.141 ms
```

### Course - Case1 (GiST)

```sql
-- 실행 쿼리
SELECT count(*)
FROM course c
WHERE c.is_public = true
  AND EXISTS (
      SELECT 1
      FROM pin p
      JOIN place pl ON p.place_id = pl.id
      WHERE p.course_id = c.id
        AND ST_Intersects(
            pl.location::geometry,
            ST_MakeEnvelope(126.9983, 37.4758, 127.0553, 37.5208, 4326)
        )
  );

-- 실행계획 분석 결과(간소화)
-- 주요 노드: Aggregate -> Nested Loop
-- 내부: HashAggregate -> Parallel Hash Join(pin, place) -> Index Scan(course_pkey)
-- place 접근: Parallel Bitmap Heap/Index Scan
-- Execution Time: 27.465 ms
```

### Course - Case2 (GiST+MBR)

```sql
-- 실행 쿼리
SELECT count(*)
FROM course c
WHERE c.is_public = true
  AND EXISTS (
      SELECT 1
      FROM pin p
      JOIN place pl ON p.place_id = pl.id
      WHERE p.course_id = c.id
        AND pl.location::geometry && ST_MakeEnvelope(126.9983, 37.4758, 127.0553, 37.5208, 4326)
        AND ST_Intersects(
            pl.location::geometry,
            ST_MakeEnvelope(126.9983, 37.4758, 127.0553, 37.5208, 4326)
        )
  );

-- 실행계획 분석 결과(간소화)
-- 주요 노드: Aggregate -> Nested Loop
-- 내부: HashAggregate -> Bitmap Heap/Index Scan(place) -> Index Scan(pin idx_pin_place_id) -> Index Scan(course_pkey)
-- Recheck Cond 포함, 후보군 축소 효과 확인
-- Execution Time: 15.210 ms
```

## 3. 넓은 시나리오

### Place - Baseline

```sql
-- 실행 쿼리
SELECT count(*)
FROM place p
WHERE ST_Intersects(
    p.location::geometry,
    ST_MakeEnvelope(126.9755, 37.4578, 127.0781, 37.5388, 4326)
);

-- 실행계획 분석 결과(간소화)
-- 주요 노드: Finalize Aggregate -> Gather -> Partial Aggregate -> Parallel Seq Scan on place
-- Rows Removed by Filter: 31,560 (worker당)
-- Execution Time: 111.402 ms
```

### Place - Case1 (GiST)

```sql
-- 실행 쿼리
SELECT count(*)
FROM place p
WHERE ST_Intersects(
    p.location::geometry,
    ST_MakeEnvelope(126.9755, 37.4578, 127.0781, 37.5388, 4326)
);

-- 실행계획 분석 결과(간소화)
-- 주요 노드: Finalize Aggregate -> Gather -> Partial Aggregate -> Parallel Bitmap Heap Scan
-- 인덱스 노드: Bitmap Index Scan(idx_place_location_geom_gist)
-- 후보군 5,323 rows, Rows Removed by Filter: 1
-- Execution Time: 30.647 ms
```

### Place - Case2 (GiST+MBR)

```sql
-- 실행 쿼리
SELECT count(*)
FROM place p
WHERE p.location::geometry && ST_MakeEnvelope(126.9755, 37.4578, 127.0781, 37.5388, 4326)
  AND ST_Intersects(
      p.location::geometry,
      ST_MakeEnvelope(126.9755, 37.4578, 127.0781, 37.5388, 4326)
  );

-- 실행계획 분석 결과(간소화)
-- 주요 노드: Aggregate -> Bitmap Heap Scan -> Bitmap Index Scan(idx_place_location_geom_gist)
-- Recheck Cond 포함, Rows Removed by Filter: 2
-- Execution Time: 4.232 ms
```

### Course - Baseline

```sql
-- 실행 쿼리
SELECT count(*)
FROM course c
WHERE c.is_public = true
  AND EXISTS (
      SELECT 1
      FROM pin p
      JOIN place pl ON p.place_id = pl.id
      WHERE p.course_id = c.id
        AND ST_Intersects(
            pl.location::geometry,
            ST_MakeEnvelope(126.9755, 37.4578, 127.0781, 37.5388, 4326)
        )
  );

-- 실행계획 분석 결과(간소화)
-- 주요 노드: Aggregate -> Nested Loop Semi Join
-- 내부: course Seq Scan + place Parallel Seq Scan + pin Index Scan(idx_pin_place_id)
-- Rows Removed by Join Filter: 135,996,671
-- Execution Time: 7,463.794 ms
```

### Course - Case1 (GiST)

```sql
-- 실행 쿼리
SELECT count(*)
FROM course c
WHERE c.is_public = true
  AND EXISTS (
      SELECT 1
      FROM pin p
      JOIN place pl ON p.place_id = pl.id
      WHERE p.course_id = c.id
        AND ST_Intersects(
            pl.location::geometry,
            ST_MakeEnvelope(126.9755, 37.4578, 127.0781, 37.5388, 4326)
        )
  );

-- 실행계획 분석 결과(간소화)
-- 주요 노드: Aggregate -> Hash Semi Join
-- 내부: Parallel Hash Join(pin, place) + course Seq Scan
-- place 접근: Parallel Bitmap Heap/Index Scan
-- Execution Time: 35.695 ms
```

### Course - Case2 (GiST+MBR)

```sql
-- 실행 쿼리
SELECT count(*)
FROM course c
WHERE c.is_public = true
  AND EXISTS (
      SELECT 1
      FROM pin p
      JOIN place pl ON p.place_id = pl.id
      WHERE p.course_id = c.id
        AND pl.location::geometry && ST_MakeEnvelope(126.9755, 37.4578, 127.0781, 37.5388, 4326)
        AND ST_Intersects(
            pl.location::geometry,
            ST_MakeEnvelope(126.9755, 37.4578, 127.0781, 37.5388, 4326)
        )
  );

-- 실행계획 분석 결과(간소화)
-- 주요 노드: Aggregate -> Nested Loop
-- 내부: HashAggregate -> Bitmap Heap/Index Scan(place) -> Index Scan(pin idx_pin_place_id) -> Index Scan(course_pkey)
-- Recheck Cond 포함, 후보군 축소 효과 확인
-- Execution Time: 16.935 ms
```
