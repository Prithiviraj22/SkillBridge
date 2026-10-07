package com.skillbridge.controller;

import com.skillbridge.entity.Enrollment;
import com.skillbridge.entity.Skill;
import com.skillbridge.repository.EnrollmentRepository;
import com.skillbridge.repository.SkillRepository;
import com.skillbridge.entity.LearningPlan;
import com.skillbridge.repository.LearningPlanRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    private final EnrollmentRepository enrollmentRepository;
    private final SkillRepository skillRepository;
    private final LearningPlanRepository learningPlanRepository;
    private final ObjectMapper objectMapper;

    public EnrollmentController(EnrollmentRepository enrollmentRepository, SkillRepository skillRepository, LearningPlanRepository learningPlanRepository, ObjectMapper objectMapper) {
        this.enrollmentRepository = enrollmentRepository;
        this.skillRepository = skillRepository;
        this.learningPlanRepository = learningPlanRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/student/{studentId}")
    public List<Map<String, Object>> getStudentEnrollments(@PathVariable Long studentId) {
        return enrollmentRepository.findByStudentId(studentId).stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping
    public ResponseEntity<?> enroll(@RequestBody EnrollmentRequest request) {
        if (request.studentId == null || request.skillId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "studentId and skillId are required"));
        }

        if (!skillRepository.existsById(request.skillId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Skill not found"));
        }

        if (enrollmentRepository.findByStudentIdAndSkillId(request.studentId, request.skillId).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Already enrolled in this skill"));
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setStudentId(request.studentId);
        enrollment.setSkillId(request.skillId);
        enrollment.setStatus("ACTIVE");
        enrollment.setEnrolledAt(LocalDateTime.now());

        Enrollment saved = enrollmentRepository.save(enrollment);
        if (learningPlanRepository.findByStudentIdAndSkillId(request.studentId, request.skillId).isEmpty()) {
            Skill skill = skillRepository.findById(request.skillId).orElseThrow();
            LearningPlan plan = new LearningPlan();
            plan.setStudentId(request.studentId); plan.setMentorId(skill.getTeacherId()); plan.setSkillId(skill.getId());
            plan.setTitle(skill.getName() + " Learning Plan");
            plan.setDescription("Automatically generated plan. Learner and mentor can customize every stage.");
            try {
                java.util.List<java.util.Map<String,Object>> segs = new java.util.ArrayList<>();
                for (String title : templateTitles(skill.getName())) {
                    segs.add(new java.util.LinkedHashMap<>(java.util.Map.of("title", title, "description", "Complete this stage of the " + skill.getName() + " learning path.", "completed", false)));
                }
                plan.setSegmentsJson(objectMapper.writeValueAsString(segs));
            } catch (Exception ignored) {}
            learningPlanRepository.save(plan);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> request) {
        return enrollmentRepository.findById(id).map(enrollment -> {
            String status = request.get("status");
            if (status == null || status.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "status is required"));
            }
            enrollment.setStatus(status.toUpperCase());
            return ResponseEntity.ok(toResponse(enrollmentRepository.save(enrollment)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        if (!enrollmentRepository.existsById(id)) return ResponseEntity.notFound().build();
        enrollmentRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private java.util.List<String> templateTitles(String name) {
        String n = name.toLowerCase();
        if (n.contains("java")) return java.util.List.of("Java fundamentals","Control statements and methods","Arrays and Strings","Object-Oriented Programming","Exception handling","Collections Framework","Multithreading","JDBC and MySQL","Build a Java mini project");
        if (n.contains("react")) return java.util.List.of("JSX and components","Props and component design","State and events","useEffect and hooks","Forms and validation","React Router","API integration","State management","Build a React project");
        if (n.contains("python")) return java.util.List.of("Python syntax and variables","Conditions and loops","Functions","Lists, tuples and dictionaries","OOP in Python","Modules and packages","File handling and exceptions","Libraries and APIs","Build a Python project");
        if (n.contains("javascript")) return java.util.List.of("Syntax and variables","Functions and scope","Arrays and objects","DOM manipulation","ES6 features","Promises and async/await","APIs and fetch","Build a JavaScript project");
        if (n.contains("sql") || n.contains("mysql")) return java.util.List.of("Database fundamentals","CREATE and ALTER","INSERT, UPDATE and DELETE","SELECT and filtering","Joins","Subqueries and aggregation","Views and constraints","Build a database project");
        return java.util.List.of("Understand fundamentals","Learn core concepts","Practice guided exercises","Complete intermediate tasks","Apply advanced concepts","Build a practical mini project","Review and improve project","Complete final assessment");
    }

    private Map<String, Object> toResponse(Enrollment enrollment) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", enrollment.getId());
        response.put("studentId", enrollment.getStudentId());
        response.put("skillId", enrollment.getSkillId());
        response.put("status", enrollment.getStatus());
        response.put("enrolledAt", enrollment.getEnrolledAt());

        skillRepository.findById(enrollment.getSkillId()).ifPresent(skill -> {
            response.put("skillName", skill.getName());
            response.put("category", skill.getCategory());
            response.put("level", skill.getLevel());
        });

        return response;
    }

    public static class EnrollmentRequest {
        public Long studentId;
        public Long skillId;
    }
}
