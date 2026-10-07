package com.skillbridge.repository;
import com.skillbridge.entity.LearningPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface LearningPlanRepository extends JpaRepository<LearningPlan,Long>{
 List<LearningPlan> findByStudentIdOrderByIdDesc(Long id);
 List<LearningPlan> findByMentorIdOrderByIdDesc(Long id);
 Optional<LearningPlan> findByStudentIdAndSkillId(Long studentId,Long skillId);
}
