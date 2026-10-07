package com.skillbridge.repository;

import com.skillbridge.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByStudentId(Long studentId);
    List<Enrollment> findBySkillId(Long skillId);
    Optional<Enrollment> findByStudentIdAndSkillId(Long studentId, Long skillId);
}
