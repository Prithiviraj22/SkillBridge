package com.skillbridge.controller;

import com.skillbridge.entity.Skill;
import com.skillbridge.entity.SkillExchange;
import com.skillbridge.entity.StudentSkill;
import com.skillbridge.repository.SkillExchangeRepository;
import com.skillbridge.repository.SkillRepository;
import com.skillbridge.repository.StudentSkillRepository;
import com.skillbridge.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/skill-exchanges")
public class SkillExchangeController {

    private final SkillExchangeRepository repo;
    private final SkillRepository skills;
    private final StudentSkillRepository studentSkills;
    private final UserRepository users;

    public SkillExchangeController(
            SkillExchangeRepository repo,
            SkillRepository skills,
            StudentSkillRepository studentSkills,
            UserRepository users) {
        this.repo = repo;
        this.skills = skills;
        this.studentSkills = studentSkills;
        this.users = users;
    }

    @GetMapping("/student/{id}")
    public List<Map<String, Object>> mine(@PathVariable Long id) {
        return repo.findByRequesterIdOrderByCreatedAtDesc(id)
                .stream()
                .map(this::response)
                .toList();
    }

    @GetMapping("/incoming/{id}")
    public List<Map<String, Object>> incoming(@PathVariable Long id) {
        return repo.findByRecipientIdOrderByCreatedAtDesc(id)
                .stream()
                .map(this::response)
                .toList();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ExchangeRequest r) {
        if (r.requesterId == null || r.recipientId == null) {
            return bad("Requester and recipient are required");
        }

        if (r.requesterId.equals(r.recipientId)) {
            return bad("You cannot send an exchange request to yourself");
        }

        if (!users.existsById(r.requesterId) || !users.existsById(r.recipientId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "User not found"));
        }

        String offeredName = null;

        if (r.offeredSkillId != null) {
            StudentSkill ownSkill = studentSkills.findById(r.offeredSkillId).orElse(null);

            if (ownSkill != null) {
                if (!r.requesterId.equals(ownSkill.getStudentId())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message", "You can only offer one of your own skills"));
                }
                offeredName = ownSkill.getName();
            } else {
                Skill teacherSkill = skills.findById(r.offeredSkillId).orElse(null);
                if (teacherSkill == null || teacherSkill.getTeacherId() == null ||
                        !r.requesterId.equals(teacherSkill.getTeacherId())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message", "You can only offer one of your own skills"));
                }
                offeredName = teacherSkill.getName();
            }
        }

        if (hasText(r.offeredSkillName)) {
            offeredName = r.offeredSkillName.trim();
        }

        if (!hasText(offeredName)) {
            return bad("Please select or type the skill you can offer");
        }

        String requestedName = null;

        if (r.requestedSkillId != null) {
            Skill requested = skills.findById(r.requestedSkillId).orElse(null);

            if (requested == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Requested skill was not found"));
            }

            requestedName = requested.getName();
        }

        if (hasText(r.requestedSkillName)) {
            requestedName = r.requestedSkillName.trim();
        }

        if (!hasText(requestedName)) {
            return bad("Please select or type the skill you want to learn");
        }

        if (offeredName.equalsIgnoreCase(requestedName)) {
            return bad("Choose two different skills");
        }

        SkillExchange exchange = new SkillExchange();
        exchange.setRequesterId(r.requesterId);
        exchange.setRecipientId(r.recipientId);
        exchange.setOfferedSkillId(r.offeredSkillId);
        exchange.setRequestedSkillId(r.requestedSkillId);
        exchange.setOfferedSkillName(offeredName);
        exchange.setRequestedSkillName(requestedName);
        exchange.setStatus("PENDING");

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response(repo.save(exchange)));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> status(
            @PathVariable Long id,
            @RequestBody StatusRequest r) {

        return repo.findById(id)
                .map(exchange -> {
                    if (r.userId == null ||
                            (!r.userId.equals(exchange.getRecipientId()) &&
                             !r.userId.equals(exchange.getRequesterId()))) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(Map.of("message", "You cannot update this exchange"));
                    }

                    String status = r.status == null
                            ? "PENDING"
                            : r.status.toUpperCase();

                    if (("ACCEPTED".equals(status) || "REJECTED".equals(status)) &&
                            !r.userId.equals(exchange.getRecipientId())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(Map.of("message", "Only the recipient can accept or reject this request"));
                    }

                    exchange.setStatus(status);

                    return ResponseEntity.ok(response(repo.save(exchange)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private Map<String, Object> response(SkillExchange e) {
        Map<String, Object> m = new LinkedHashMap<>();

        m.put("id", e.getId());
        m.put("requesterId", e.getRequesterId());
        m.put("recipientId", e.getRecipientId());
        m.put("offeredSkillId", e.getOfferedSkillId());
        m.put("requestedSkillId", e.getRequestedSkillId());
        m.put("status", e.getStatus());
        m.put("createdAt", e.getCreatedAt());

        String offered = e.getOfferedSkillName();
        String requested = e.getRequestedSkillName();

        if (!hasText(offered) && e.getOfferedSkillId() != null) {
            StudentSkill ownSkill = studentSkills.findById(e.getOfferedSkillId()).orElse(null);
            if (ownSkill != null) {
                offered = ownSkill.getName();
            } else {
                Skill teacherSkill = skills.findById(e.getOfferedSkillId()).orElse(null);
                if (teacherSkill != null) offered = teacherSkill.getName();
            }
        }

        if (!hasText(requested) && e.getRequestedSkillId() != null) {
            Skill skill = skills.findById(e.getRequestedSkillId()).orElse(null);
            if (skill != null) requested = skill.getName();
        }

        m.put("offeredSkillName", offered);
        m.put("requestedSkillName", requested);

        users.findById(e.getRequesterId()).ifPresent(u -> {
            m.put("requesterName", u.getName());
            m.put("requesterRole", u.getRole());
        });

        users.findById(e.getRecipientId()).ifPresent(u -> {
            m.put("recipientName", u.getName());
            m.put("recipientRole", u.getRole());
        });

        return m;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private ResponseEntity<Map<String, String>> bad(String message) {
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    public static class ExchangeRequest {
        public Long requesterId;
        public Long recipientId;
        public Long offeredSkillId;
        public Long requestedSkillId;
        public String offeredSkillName;
        public String requestedSkillName;
    }

    public static class StatusRequest {
        public Long userId;
        public String status;
    }
}
