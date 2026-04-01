package com.meetup.hereandnow.course.infrastructure.repository;

import com.meetup.hereandnow.course.domain.entity.Course;
import com.meetup.hereandnow.member.domain.Member;
import jakarta.persistence.LockModeType;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Course c WHERE c.id = :id")
    Optional<Course> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT c.id FROM Course c WHERE c.member = :member ORDER BY c.createdAt DESC")
    Page<Long> findCourseIdsByMember(Member member, Pageable pageable);

    @Query("SELECT DISTINCT c FROM Course c LEFT JOIN FETCH c.pinList WHERE c.id IN :ids ORDER BY c.createdAt DESC")
    List<Course> findWithPinsByIds(@Param("ids") List<Long> ids);

    @Query(value = "SELECT * FROM course c WHERE c.member_id = (:memberId) ORDER BY c.created_at DESC LIMIT 1",
            nativeQuery = true)
    Optional<Course> findByMemberOrderByCreatedAtDesc(@Param("memberId") Long memberId);

    @Query("""
            SELECT DISTINCT c FROM Course c
            LEFT JOIN FETCH c.pinList p
            LEFT JOIN FETCH p.place pl
            WHERE c.id = :courseId
            ORDER BY p.id ASC
            """)
    Optional<Course> findCourseDetailsById(@Param("courseId") Long courseId);

    List<Course> findByCourseVisitMemberAndMemberIn(String courseVisitMember, List<Member> members);

    @Query(
            value = """
                    SELECT c.id FROM course c
                    WHERE c.is_public = true
                    AND EXISTS (
                        SELECT 1 FROM pin p
                        JOIN place pl ON p.place_id = pl.id
                        WHERE p.course_id = c.id
                        AND pl.location::geometry && ST_MakeEnvelope(
                            ST_X(CAST(:point AS geometry)) - (1500.0 / (111320.0 * GREATEST(COS(RADIANS(ST_Y(CAST(:point AS geometry)))), 0.01))),
                            ST_Y(CAST(:point AS geometry)) - (1500.0 / 110574.0),
                            ST_X(CAST(:point AS geometry)) + (1500.0 / (111320.0 * GREATEST(COS(RADIANS(ST_Y(CAST(:point AS geometry)))), 0.01))),
                            ST_Y(CAST(:point AS geometry)) + (1500.0 / 110574.0),
                            4326
                        )
                        AND ST_DWithin(pl.location, :point, 1500)
                    )
                    """,
            countQuery = """
                    SELECT count(*) FROM course c
                    WHERE c.is_public = true
                    AND EXISTS (
                        SELECT 1 FROM pin p
                        JOIN place pl ON p.place_id = pl.id
                        WHERE p.course_id = c.id
                        AND pl.location::geometry && ST_MakeEnvelope(
                            ST_X(CAST(:point AS geometry)) - (1500.0 / (111320.0 * GREATEST(COS(RADIANS(ST_Y(CAST(:point AS geometry)))), 0.01))),
                            ST_Y(CAST(:point AS geometry)) - (1500.0 / 110574.0),
                            ST_X(CAST(:point AS geometry)) + (1500.0 / (111320.0 * GREATEST(COS(RADIANS(ST_Y(CAST(:point AS geometry)))), 0.01))),
                            ST_Y(CAST(:point AS geometry)) + (1500.0 / 110574.0),
                            4326
                        )
                        AND ST_DWithin(pl.location, :point, 1500)
                    )
                    """,
            nativeQuery = true
    )
    Page<Long> findNearbyCourseIds(@Param("point") Point point, Pageable pageable);

    @Query(
            value = """
                    WITH nearby_course AS (
                        SELECT c.id FROM course c
                        WHERE c.is_public = true
                        AND EXISTS (
                            SELECT 1 FROM pin p
                            JOIN place pl ON p.place_id = pl.id
                            WHERE p.course_id = c.id
                            AND pl.location::geometry && ST_MakeEnvelope(
                                ST_X(CAST(:point AS geometry)) - (1500.0 / (111320.0 * GREATEST(COS(RADIANS(ST_Y(CAST(:point AS geometry)))), 0.01))),
                                ST_Y(CAST(:point AS geometry)) - (1500.0 / 110574.0),
                                ST_X(CAST(:point AS geometry)) + (1500.0 / (111320.0 * GREATEST(COS(RADIANS(ST_Y(CAST(:point AS geometry)))), 0.01))),
                                ST_Y(CAST(:point AS geometry)) + (1500.0 / 110574.0),
                                4326
                            )
                            AND ST_DWithin(pl.location, :point, 1500)
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
                    """,
            countQuery = """
                    SELECT count(*) FROM course c
                    WHERE c.is_public = true
                    AND EXISTS (
                        SELECT 1 FROM pin p
                        JOIN place pl ON p.place_id = pl.id
                        WHERE p.course_id = c.id
                        AND pl.location::geometry && ST_MakeEnvelope(
                            ST_X(CAST(:point AS geometry)) - (1500.0 / (111320.0 * GREATEST(COS(RADIANS(ST_Y(CAST(:point AS geometry)))), 0.01))),
                            ST_Y(CAST(:point AS geometry)) - (1500.0 / 110574.0),
                            ST_X(CAST(:point AS geometry)) + (1500.0 / (111320.0 * GREATEST(COS(RADIANS(ST_Y(CAST(:point AS geometry)))), 0.01))),
                            ST_Y(CAST(:point AS geometry)) + (1500.0 / 110574.0),
                            4326
                        )
                        AND ST_DWithin(pl.location, :point, 1500)
                    )
                    """,
            nativeQuery = true
    )
    Page<Long> findNearbyCourseIdsSortedByCommentCount(@Param("point") Point point, Pageable pageable);

    @Query("SELECT c FROM Course c JOIN FETCH c.member m WHERE c.id IN :courseIds")
    List<Course> findCoursesWithDetailsByIds(@Param("courseIds") List<Long> courseIds);

    @Modifying
    @Query("UPDATE Course c SET c.viewCount = c.viewCount + 1 WHERE c.id = :courseId")
    void increaseViewCount(@Param("courseId") Long courseId);

    @Query(
            """
                         SELECT c FROM Course c
                         WHERE c.member = :member AND c.courseVisitMember = :visitMember
                         ORDER BY c.createdAt DESC
                         LIMIT 1
                    """
    )
    Optional<Course> findLatestCourse(
            @Param("member") Member member,
            @Param("visitMember") String visitMember
    );

    @Query(value = "SELECT c FROM Course c JOIN FETCH c.member m WHERE c.isPublic = true",
            countQuery = "SELECT COUNT(c) FROM Course c WHERE c.isPublic = true")
    Page<Course> findCoursesWithMember(Pageable pageable);
}

