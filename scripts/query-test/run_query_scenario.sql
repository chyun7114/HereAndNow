\set ON_ERROR_STOP on

\echo '============================================================'
\echo 'SCENARIO: ' :scenario_name
\echo 'BBOX    : ' :min_lon ',' :min_lat ' ~ ' :max_lon ',' :max_lat
\echo '============================================================'

\echo ''
\echo '[PLACE][BASELINE] index drop + explain'
DROP INDEX IF EXISTS idx_place_location_geom_gist;
ANALYZE place;
EXPLAIN (ANALYZE, BUFFERS)
SELECT count(*)
FROM place p
WHERE ST_Intersects(
    p.location::geometry,
    ST_MakeEnvelope(:min_lon, :min_lat, :max_lon, :max_lat, 4326)
);

\echo ''
\echo '[PLACE][CASE1_GIST] index create + explain'

CREATE INDEX IF NOT EXISTS idx_place_location_geom_gist
    ON place USING GIST ((location::geometry));
ANALYZE place;
EXPLAIN (ANALYZE, BUFFERS)
SELECT count(*)
FROM place p
WHERE ST_Intersects(
    p.location::geometry,
    ST_MakeEnvelope(:min_lon, :min_lat, :max_lon, :max_lat, 4326)
);

\echo ''
\echo '[PLACE][CASE2_GIST_MBR] explain'
EXPLAIN (ANALYZE, BUFFERS)
SELECT count(*)
FROM place p
WHERE p.location::geometry && ST_MakeEnvelope(:min_lon, :min_lat, :max_lon, :max_lat, 4326)
  AND ST_Intersects(
      p.location::geometry,
      ST_MakeEnvelope(:min_lon, :min_lat, :max_lon, :max_lat, 4326)
  );

\echo ''
\echo '[COURSE][BASELINE] index drop + explain'
DROP INDEX IF EXISTS idx_place_location_geom_gist;
ANALYZE place;
ANALYZE pin;
ANALYZE course;
EXPLAIN (ANALYZE, BUFFERS)
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
            ST_MakeEnvelope(:min_lon, :min_lat, :max_lon, :max_lat, 4326)
        )
  );

\echo ''
\echo '[COURSE][CASE1_GIST] index create + explain'
CREATE INDEX IF NOT EXISTS idx_place_location_geom_gist
    ON place USING GIST ((location::geometry));
ANALYZE place;
EXPLAIN (ANALYZE, BUFFERS)
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
            ST_MakeEnvelope(:min_lon, :min_lat, :max_lon, :max_lat, 4326)
        )
  );

\echo ''
\echo '[COURSE][CASE2_GIST_MBR] explain'
EXPLAIN (ANALYZE, BUFFERS)
SELECT count(*)
FROM course c
WHERE c.is_public = true
  AND EXISTS (
      SELECT 1
      FROM pin p
      JOIN place pl ON p.place_id = pl.id
      WHERE p.course_id = c.id
        AND pl.location::geometry && ST_MakeEnvelope(:min_lon, :min_lat, :max_lon, :max_lat, 4326)
        AND ST_Intersects(
            pl.location::geometry,
            ST_MakeEnvelope(:min_lon, :min_lat, :max_lon, :max_lat, 4326)
        )
  );

