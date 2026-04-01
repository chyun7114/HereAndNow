\set ON_ERROR_STOP on
\echo '=== CURRENT_QUERY_PLACE_10K ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT p.id
FROM place p
WHERE p.location::geometry && ST_MakeEnvelope(
    127.026842105662 - (1500.0 / (111320.0 * GREATEST(COS(RADIANS(37.4982667167977)), 0.01))),
    37.4982667167977 - (1500.0 / 110574.0),
    127.026842105662 + (1500.0 / (111320.0 * GREATEST(COS(RADIANS(37.4982667167977)), 0.01))),
    37.4982667167977 + (1500.0 / 110574.0),
    4326
)
AND ST_DWithin(
    p.location,
    ST_SetSRID(ST_MakePoint(127.026842105662, 37.4982667167977), 4326)::geography,
    1500
);

\echo '=== CURRENT_QUERY_COURSE_REVIEWS_10K ==='
EXPLAIN (ANALYZE, BUFFERS)
WITH center AS (
    SELECT ST_SetSRID(ST_MakePoint(127.026842105662, 37.4982667167977), 4326)::geography AS point
), nearby_course AS (
    SELECT c.id
    FROM course c, center
    WHERE c.is_public = true
      AND EXISTS (
          SELECT 1
          FROM pin p
          JOIN place pl ON p.place_id = pl.id
          WHERE p.course_id = c.id
            AND pl.location::geometry && ST_MakeEnvelope(
                ST_X(CAST(center.point AS geometry)) - (1500.0 / (111320.0 * GREATEST(COS(RADIANS(ST_Y(CAST(center.point AS geometry)))), 0.01))),
                ST_Y(CAST(center.point AS geometry)) - (1500.0 / 110574.0),
                ST_X(CAST(center.point AS geometry)) + (1500.0 / (111320.0 * GREATEST(COS(RADIANS(ST_Y(CAST(center.point AS geometry)))), 0.01))),
                ST_Y(CAST(center.point AS geometry)) + (1500.0 / 110574.0),
                4326
            )
            AND ST_DWithin(pl.location, center.point, 1500)
      )
)
SELECT nc.id
FROM nearby_course nc
LEFT JOIN LATERAL (
    SELECT COUNT(*) AS comment_count
    FROM course_comment cc
    WHERE cc.course_id = nc.id
) comment_stat ON true
ORDER BY COALESCE(comment_stat.comment_count, 0) DESC, nc.id DESC
LIMIT 20;
