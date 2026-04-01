# 쿼리 실행계획 상세 로그

본 문서는 `query-test-final-report.md`에서 분리한 실행 쿼리/간소 실행계획 상세 기록이다.

## 5. 시나리오별 쿼리 + 실행계획 분석 결과(간소화)

## 5.1 좁은 시나리오

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
-- 주요 노드: Aggregate -> Seq Scan on place
-- 필터: ST_Intersects(location::geometry, envelope)
-- Rows Removed by Filter: 9,975
-- Execution Time: 129.937 ms
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
-- Heap Blocks: exact=25
-- Execution Time: 0.254 ms
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
-- Filter: ST_Intersects(...)
-- Execution Time: 0.137 ms
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
-- 내부: place Seq Scan + pin Seq Scan + Nested Loop
-- Rows Removed by Join Filter: 69,675
-- Execution Time: 41.299 ms
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
-- 내부: HashAggregate -> Nested Loop -> Bitmap Heap/Index Scan(place) + Index Scan(pin idx_pin_place_id)
-- Execution Time: 0.291 ms
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
-- 내부: HashAggregate -> Nested Loop -> Index Scan(place idx_place_location_geom_gist) + Index Scan(pin idx_pin_place_id)
-- Execution Time: 0.171 ms
```

## 5.2 일반 시나리오

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
-- 주요 노드: Aggregate -> Seq Scan on place
-- Rows Removed by Filter: 9,836
-- Execution Time: 16.983 ms
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
-- 주요 노드: Aggregate -> Bitmap Heap Scan -> Bitmap Index Scan(idx_place_location_geom_gist)
-- Index Cond: location::geometry && envelope
-- Execution Time: 0.401 ms
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
-- Recheck Cond: location::geometry && envelope
-- Execution Time: 0.239 ms
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
-- 내부: place Seq Scan + pin Seq Scan + Nested Loop
-- Rows Removed by Join Filter: 446,292
-- Execution Time: 199.219 ms
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
-- 내부: HashAggregate -> Hash Join(pin, place) + course_pkey Index Scan
-- place 접근: Bitmap Heap/Index Scan(idx_place_location_geom_gist)
-- Execution Time: 1.696 ms
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
-- 내부: HashAggregate -> Nested Loop(place, pin idx_pin_place_id) + course_pkey Index Scan
-- place 접근: Bitmap Heap/Index Scan(idx_place_location_geom_gist), Recheck Cond 포함
-- Execution Time: 0.722 ms
```

## 5.3 넓은 시나리오

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
-- 주요 노드: Aggregate -> Seq Scan on place
-- Rows Removed by Filter: 9,480
-- Execution Time: 27.117 ms
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
-- 주요 노드: Aggregate -> Bitmap Heap Scan -> Bitmap Index Scan(idx_place_location_geom_gist)
-- Index Cond: location::geometry && envelope
-- Execution Time: 0.961 ms
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
-- Recheck Cond: location::geometry && envelope
-- Execution Time: 0.475 ms
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
-- 내부: place Seq Scan + pin Seq Scan + Nested Loop
-- Rows Removed by Join Filter: 1,335,536
-- Execution Time: 618.473 ms
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
-- 내부: Hash Join(pin, place) + place Bitmap Heap/Index Scan + course Seq Scan
-- Execution Time: 2.279 ms
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
-- 내부: HashAggregate -> Nested Loop(place Bitmap Heap/Index, pin idx_pin_place_id) + course_pkey Index Scan
-- Execution Time: 2.795 ms
```

---
