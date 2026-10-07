package com.skillbridge.controller;

import com.skillbridge.entity.MentorRequest;
import com.skillbridge.entity.Skill;
import com.skillbridge.entity.User;
import com.skillbridge.repository.MentorRequestRepository;
import com.skillbridge.repository.SkillRepository;
import com.skillbridge.repository.UserRepository;
import com.skillbridge.repository.EnrollmentRepository;
import com.skillbridge.entity.Enrollment;
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
@RequestMapping("/api/mentor-requests")
public class MentorRequestController {
    private final MentorRequestRepository requestRepository;
    private final SkillRepository skillRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LearningPlanRepository learningPlanRepository;
    private final ObjectMapper objectMapper;

    public MentorRequestController(MentorRequestRepository requestRepository,
                                    SkillRepository skillRepository,
                                    UserRepository userRepository,
                                    EnrollmentRepository enrollmentRepository,
                                    LearningPlanRepository learningPlanRepository,
                                    ObjectMapper objectMapper) {
        this.requestRepository = requestRepository;
        this.skillRepository = skillRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.learningPlanRepository = learningPlanRepository;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateRequest body) {
        if (body.studentId == null || body.skillId == null)
            return ResponseEntity.badRequest().body(Map.of("message", "studentId and skillId are required"));

        User student = userRepository.findById(body.studentId).orElse(null);
        Skill skill = skillRepository.findById(body.skillId).orElse(null);
        if (student == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Student not found"));
        if (skill == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Skill not found"));
        if (skill.getTeacherId() == null) return ResponseEntity.badRequest().body(Map.of("message", "This skill has no mentor assigned"));
        if (skill.getTeacherId().equals(body.studentId)) return ResponseEntity.badRequest().body(Map.of("message", "You cannot request your own skill"));
        if (requestRepository.findByStudentIdAndSkillIdAndStatus(body.studentId, body.skillId, "PENDING").isPresent())
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "A request is already pending"));
        if (requestRepository.findByStudentIdAndSkillIdAndStatus(body.studentId, body.skillId, "ACCEPTED").isPresent())
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "You already have an accepted session for this skill"));

        MentorRequest request = new MentorRequest();
        request.setStudentId(body.studentId);
        request.setTeacherId(skill.getTeacherId());
        request.setSkillId(body.skillId);
        request.setStatus("PENDING");
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(requestRepository.save(request)));
    }

    @GetMapping("/teacher/{teacherId}")
    public List<Map<String,Object>> teacherRequests(@PathVariable Long teacherId) {
        return requestRepository.findByTeacherIdOrderByCreatedAtDesc(teacherId).stream().map(r -> { ensureLearningPlan(r); return toResponse(r); }).toList();
    }

    @GetMapping("/student/{studentId}")
    public List<Map<String,Object>> studentRequests(@PathVariable Long studentId) {
        return requestRepository.findByStudentIdOrderByCreatedAtDesc(studentId).stream().map(r -> { ensureLearningPlan(r); return toResponse(r); }).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return requestRepository.findById(id).map(r -> ResponseEntity.ok(toResponse(r)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/schedule")
    public ResponseEntity<?> acceptAndSchedule(@PathVariable Long id, @RequestBody ScheduleRequest body) {
        if (body.teacherId == null || body.scheduledAt == null)
            return ResponseEntity.badRequest().body(Map.of("message", "teacherId and scheduledAt are required"));
        if (body.scheduledAt.isBefore(LocalDateTime.now()))
            return ResponseEntity.badRequest().body(Map.of("message", "Session time must be in the future"));

        return requestRepository.findById(id).map(request -> {
            if (!request.getTeacherId().equals(body.teacherId))
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "You cannot manage this request"));
            if (!"PENDING".equalsIgnoreCase(request.getStatus()))
                return ResponseEntity.badRequest().body(Map.of("message", "Only pending requests can be scheduled"));

            request.setStatus("ACCEPTED");
            request.setScheduledAt(body.scheduledAt);
            if (enrollmentRepository.findByStudentIdAndSkillId(request.getStudentId(), request.getSkillId()).isEmpty()) {
                Enrollment enrollment = new Enrollment();
                enrollment.setStudentId(request.getStudentId());
                enrollment.setSkillId(request.getSkillId());
                enrollment.setStatus("ACTIVE");
                enrollment.setEnrolledAt(LocalDateTime.now());
                enrollmentRepository.save(enrollment);
            }
            ensureLearningPlan(request);
            return ResponseEntity.ok(toResponse(requestRepository.save(request)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable Long id, @RequestBody RoleRequest body) {
        return requestRepository.findById(id).map(request -> {
            if (body.teacherId == null || !request.getTeacherId().equals(body.teacherId))
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "You cannot manage this request"));
            request.setStatus("REJECTED");
            return ResponseEntity.ok(toResponse(requestRepository.save(request)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable Long id, @RequestBody RoleRequest body) {
        return requestRepository.findById(id).map(request -> {
            if (body.teacherId == null || !request.getTeacherId().equals(body.teacherId))
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "You cannot cancel this request"));
            if (!"ACCEPTED".equalsIgnoreCase(request.getStatus()))
                return ResponseEntity.badRequest().body(Map.of("message", "Only confirmed requests can be cancelled"));
            request.setStatus("CANCELLED");
            request.setScheduledAt(null);
            enrollmentRepository.findByStudentIdAndSkillId(request.getStudentId(), request.getSkillId())
                    .ifPresent(enrollmentRepository::delete);
            learningPlanRepository.findByStudentIdAndSkillId(request.getStudentId(), request.getSkillId())
                    .ifPresent(learningPlanRepository::delete);
            return ResponseEntity.ok(toResponse(requestRepository.save(request)));
        }).orElse(ResponseEntity.notFound().build());
    }

    private void ensureLearningPlan(MentorRequest request) {
        if (!"ACCEPTED".equalsIgnoreCase(request.getStatus())) return;
        if (learningPlanRepository.findByStudentIdAndSkillId(request.getStudentId(), request.getSkillId()).isPresent()) return;
        Skill skill = skillRepository.findById(request.getSkillId()).orElse(null);
        if (skill == null) return;

        LearningPlan plan = new LearningPlan();
        plan.setStudentId(request.getStudentId());
        plan.setMentorId(request.getTeacherId());
        plan.setSkillId(skill.getId());
        plan.setTitle(skill.getName() + " Learning Plan");
        plan.setDescription("Automatically generated plan for " + skill.getName() + ". Learner and mentor can customize every stage.");
        try {
            java.util.List<java.util.Map<String,Object>> segments = new java.util.ArrayList<>();
            for (String title : templateTitles(skill.getName())) {
                java.util.Map<String,Object> segment = new java.util.LinkedHashMap<>();
                segment.put("title", title);
                segment.put("description", "Complete this stage of the " + skill.getName() + " learning path.");
                segment.put("completed", false);
                segments.add(segment);
            }
            plan.setSegmentsJson(objectMapper.writeValueAsString(segments));
        } catch (Exception ignored) {
            plan.setSegmentsJson("[]");
        }
        learningPlanRepository.save(plan);
    }

    private java.util.List<String> templateTitles(String name) {
        String n = name.toLowerCase();
        if (n.contains("java")) return java.util.List.of("Java fundamentals", "Control statements and methods", "Arrays and Strings", "Object-Oriented Programming", "Exception handling", "Collections Framework", "Multithreading", "JDBC and MySQL", "Build a Java mini project");
        if (n.contains("react")) return java.util.List.of("JSX and components", "Props and component design", "State and events", "useEffect and hooks", "Forms and validation", "React Router", "API integration", "State management", "Build a React project");
        if (n.contains("python")) return java.util.List.of("Python syntax and variables", "Conditions and loops", "Functions", "Lists, tuples and dictionaries", "OOP in Python", "Modules and packages", "File handling and exceptions", "Libraries and APIs", "Build a Python project");
        if (n.contains("javascript")) return java.util.List.of("Syntax and variables", "Functions and scope", "Arrays and objects", "DOM manipulation", "ES6 features", "Promises and async/await", "APIs and fetch", "Build a JavaScript project");
        if (n.contains("sql") || n.contains("mysql")) return java.util.List.of("Database fundamentals", "CREATE and ALTER", "INSERT, UPDATE and DELETE", "SELECT and filtering", "Joins", "Subqueries and aggregation", "Views and constraints", "Build a database project");
        return java.util.List.of("Understand fundamentals", "Learn core concepts", "Practice guided exercises", "Complete intermediate tasks", "Apply advanced concepts", "Build a practical mini project", "Review and improve project", "Complete final assessment");
    }

    private Map<String,Object> toResponse(MentorRequest r) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id", r.getId()); m.put("studentId", r.getStudentId()); m.put("teacherId", r.getTeacherId());
        m.put("skillId", r.getSkillId()); m.put("status", r.getStatus()); m.put("scheduledAt", r.getScheduledAt());
        m.put("createdAt", r.getCreatedAt());
        skillRepository.findById(r.getSkillId()).ifPresent(s -> { m.put("skillName", s.getName()); m.put("category", s.getCategory()); m.put("level", s.getLevel()); });
        userRepository.findById(r.getStudentId()).ifPresent(u -> { m.put("studentName", u.getName()); m.put("studentEmail", u.getEmail()); });
        userRepository.findById(r.getTeacherId()).ifPresent(u -> { m.put("teacherName", u.getName()); });
        return m;
    }

    public static class CreateRequest { public Long studentId; public Long skillId; }
    public static class ScheduleRequest { public Long teacherId; public LocalDateTime scheduledAt; }
    public static class RoleRequest { public Long teacherId; }
}
