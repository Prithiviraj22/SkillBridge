package com.skillbridge.repository;

import com.skillbridge.entity.MentorRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MentorRequestRepository extends JpaRepository<MentorRequest, Long> {
    List<MentorRequest> findByTeacherIdOrderByCreatedAtDesc(Long teacherId);
    List<MentorRequest> findByStudentIdOrderByCreatedAtDesc(Long studentId);
    Optional<MentorRequest> findByStudentIdAndSkillIdAndStatus(Long studentId, Long skillId, String status);
}
