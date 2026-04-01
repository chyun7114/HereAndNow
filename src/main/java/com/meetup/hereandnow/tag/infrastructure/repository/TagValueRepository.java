package com.meetup.hereandnow.tag.infrastructure.repository;

import com.meetup.hereandnow.tag.domain.entity.TagValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TagValueRepository extends JpaRepository<TagValue, Long> {
    Optional<TagValue> findByName(String name);
}
