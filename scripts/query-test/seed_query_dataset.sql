\set ON_ERROR_STOP on

BEGIN;

TRUNCATE TABLE
    pin_tag,
    pin_image,
    pin,
    course_comment,
    course_scrap,
    place_scrap,
    course,
    place,
    tag,
    tag_value,
    place_group,
    couple_course_comment,
    couple,
    member
RESTART IDENTITY CASCADE;

COMMIT;

SELECT setseed(0.4242);

INSERT INTO member (email, nickname, profile_image, provider, provider_id, username, created_at, updated_at)
VALUES (
    'query-test-user@example.com',
    'query-test-user',
    NULL,
    'GOOGLE',
    'query-test-provider-id',
    'query-test-user',
    now(),
    now()
);

INSERT INTO place_group (code, name)
VALUES ('CT1', '카페');

WITH place_seed AS (
    SELECT
        gs AS seq,
        -- 강남역 기준(37.4982667167977, 127.026842105662) 근처에 랜덤 분포
        127.026842105662 + (random() - 0.5) * :lon_spread AS lon,
        37.4982667167977 + (random() - 0.5) * :lat_spread AS lat
    FROM generate_series(1, :place_count) AS gs
)
INSERT INTO place (
    place_name,
    place_street_name_address,
    place_number_address,
    place_url,
    place_group_id,
    place_category,
    place_rating,
    place_tags,
    location,
    pin_count,
    scrap_count,
    created_at,
    updated_at
)
SELECT
    :'dataset_prefix' || '-place-' || seq,
    '서울시 강남구 테스트로 ' || seq,
    '서울시 강남구 테스트번지 ' || seq,
    'https://example.com/place/' || seq,
    (SELECT id FROM place_group WHERE code = 'CT1'),
    '음식점 > 카페',
    round((random() * 20 + 30)::numeric / 10, 1),
    NULL,
    ST_SetSRID(ST_MakePoint(lon, lat), 4326)::geography,
    0,
    0,
    now(),
    now()
FROM place_seed;

INSERT INTO course (
    course_title,
    course_description,
    course_positive,
    course_negative,
    course_rating,
    course_region,
    course_visit_date,
    course_visit_member,
    is_public,
    scrap_count,
    view_count,
    course_tags,
    member_id,
    created_at,
    updated_at
)
SELECT
    :'dataset_prefix' || '-course-' || gs,
    'query test course description #' || gs,
    'query test positive #' || gs,
    'query test negative #' || gs,
    round((random() * 20 + 30)::numeric / 10, 1),
    '강남',
    current_date - (gs % 365),
    '연인',
    true,
    0,
    0,
    NULL,
    (SELECT id FROM member WHERE email = 'query-test-user@example.com'),
    now(),
    now()
FROM generate_series(1, :course_count) AS gs;

CREATE TEMP TABLE tmp_place_rows AS
SELECT
    p.id AS place_id,
    row_number() OVER (ORDER BY p.id) AS rn
FROM place p
WHERE p.place_name LIKE :'dataset_prefix' || '-place-%';

CREATE TEMP TABLE tmp_course_rows AS
SELECT
    c.id AS course_id,
    row_number() OVER (ORDER BY c.id) AS course_rn
FROM course c
WHERE c.course_title LIKE :'dataset_prefix' || '-course-%';

CREATE TEMP TABLE tmp_course_slots AS
SELECT
    cr.course_id,
    row_number() OVER (ORDER BY cr.course_id, s.slot_idx) AS rn
FROM tmp_course_rows cr
CROSS JOIN LATERAL generate_series(1, ((cr.course_rn - 1) % 7) + 1) AS s(slot_idx);

DO $$
DECLARE
    v_place_count bigint;
    v_slot_count bigint;
BEGIN
    SELECT count(*) INTO v_place_count FROM tmp_place_rows;
    SELECT count(*) INTO v_slot_count FROM tmp_course_slots;

    IF v_slot_count < v_place_count THEN
        RAISE EXCEPTION '핀 매핑 슬롯이 부족합니다. slot_count=%, place_count=%', v_slot_count, v_place_count;
    END IF;
END
$$;

INSERT INTO pin (
    pin_positive,
    pin_negative,
    pin_rating,
    course_id,
    place_id,
    created_at,
    updated_at
)
SELECT
    'query test pin positive #' || p.rn,
    'query test pin negative #' || p.rn,
    round((random() * 20 + 30)::numeric / 10, 1),
    s.course_id,
    p.place_id,
    now(),
    now()
FROM tmp_place_rows p
JOIN tmp_course_slots s ON s.rn = p.rn;

UPDATE place pl
SET pin_count = x.pin_count
FROM (
    SELECT place_id, count(*)::bigint AS pin_count
    FROM pin
    GROUP BY place_id
) x
WHERE pl.id = x.place_id;

CREATE INDEX IF NOT EXISTS idx_place_location_geom_gist
    ON place
    USING GIST ((location::geometry));

ANALYZE place;
ANALYZE pin;
ANALYZE course;
ANALYZE course_comment;

SELECT
    (SELECT count(*) FROM place) AS place_count,
    (SELECT count(*) FROM pin) AS pin_count,
    (SELECT count(*) FROM course) AS course_count;

