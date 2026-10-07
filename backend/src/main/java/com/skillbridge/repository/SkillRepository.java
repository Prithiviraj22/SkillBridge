package com.skillbridge.repository;

import com.skillbridge.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SkillRepository extends JpaRepository<Skill, Long> {
    List<Skill> findByCategoryIgnoreCase(String category);
    List<Skill> findByTeacherId(Long teacherId);
}
