package com.meetup.hereandnow.place.infrastructure.repository;

import com.meetup.hereandnow.place.domain.Place;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Long>, JpaSpecificationExecutor<Place> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Place p WHERE p.id = :id")
    Optional<Place> findByIdWithLock(@Param("id") Long id);

    @Query(value = "SELECT * FROM place p WHERE p.place_name = :name AND ST_X(p.location::geometry) = :lon AND ST_Y(p.location::geometry) = :lat", nativeQuery = true)
    Optional<Place> findByNameAndCoordinates(@Param("name") String name, @Param("lat") double lat, @Param("lon") double lon);

    @Query("SELECT p.id FROM Place p")
    Page<Long> findAllIds(Pageable pageable);

    // 주어진 lat, lon를 중심으로 반경 1.5km 내의 장소 목록을 조회
    @Query(
            value = """
                    SELECT * FROM place p
                    WHERE p.location::geometry && ST_MakeEnvelope(
                        :lon - (1500.0 / (111320.0 * GREATEST(COS(RADIANS(:lat)), 0.01))),
                        :lat - (1500.0 / 110574.0),
                        :lon + (1500.0 / (111320.0 * GREATEST(COS(RADIANS(:lat)), 0.01))),
                        :lat + (1500.0 / 110574.0),
                        4326
                    )
                    AND ST_DWithin(
                        p.location,
                        ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
                        1500
                    )
                    """,
            countQuery = """
                    SELECT count(*) FROM place p
                    WHERE p.location::geometry && ST_MakeEnvelope(
                        :lon - (1500.0 / (111320.0 * GREATEST(COS(RADIANS(:lat)), 0.01))),
                        :lat - (1500.0 / 110574.0),
                        :lon + (1500.0 / (111320.0 * GREATEST(COS(RADIANS(:lat)), 0.01))),
                        :lat + (1500.0 / 110574.0),
                        4326
                    )
                    AND ST_DWithin(
                        p.location,
                        ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
                        1500
                    )
                    """,
            nativeQuery = true
    )
    Page<Place> findPlacesByLocation(@Param("lat") double lat, @Param("lon") double lon, Pageable pageable);

    // 1.5km 내의 장소 id 목록 조회
    @Query(
            value = """
                    SELECT p.id FROM place p
                    WHERE p.location::geometry && ST_MakeEnvelope(
                        :lon - (1500.0 / (111320.0 * GREATEST(COS(RADIANS(:lat)), 0.01))),
                        :lat - (1500.0 / 110574.0),
                        :lon + (1500.0 / (111320.0 * GREATEST(COS(RADIANS(:lat)), 0.01))),
                        :lat + (1500.0 / 110574.0),
                        4326
                    )
                    AND ST_DWithin(
                        p.location,
                        ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
                        1500
                    )
                    """,
            nativeQuery = true
    )
    List<Long> findNearbyPlaceIds(@Param("lat") double lat, @Param("lon") double lon);
}
