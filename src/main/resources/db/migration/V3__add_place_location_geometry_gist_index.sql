CREATE INDEX IF NOT EXISTS idx_place_location_geom_gist
    ON place
    USING GIST ((location::geometry));

