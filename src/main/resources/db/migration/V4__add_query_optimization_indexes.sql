CREATE INDEX IF NOT EXISTS idx_pin_course_id
    ON pin (course_id);

CREATE INDEX IF NOT EXISTS idx_pin_course_place_id
    ON pin (course_id, place_id);

CREATE INDEX IF NOT EXISTS idx_course_comment_course_id
    ON course_comment (course_id);
