package com.skillbridge.repository;

import com.skillbridge.entity.StudentSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StudentSkillRepository extends JpaRepository<StudentSkill, Long> {
    List<StudentSkill> findByStudentIdOrderByIdDesc(Long studentId);
}
