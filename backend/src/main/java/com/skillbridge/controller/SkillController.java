package com.skillbridge.controller;

import com.skillbridge.entity.Skill;
import com.skillbridge.repository.SkillRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
public class SkillController {

    private final SkillRepository skillRepository;

    public SkillController(SkillRepository skillRepository) {
        this.skillRepository = skillRepository;
    }

    @GetMapping
    public List<Skill> getAllSkills(@RequestParam(required = false) String category) {
        if (category != null && !category.isBlank()) {
            return skillRepository.findByCategoryIgnoreCase(category);
        }
        return skillRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Skill> getSkill(@PathVariable Long id) {
        return skillRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createSkill(@RequestBody Skill skill) {
        if (skill.getName() == null || skill.getName().isBlank()
                || skill.getDescription() == null || skill.getDescription().isBlank()
                || skill.getCategory() == null || skill.getCategory().isBlank()
                || skill.getLevel() == null || skill.getLevel().isBlank()) {
            return ResponseEntity.badRequest().body("Name, description, category and level are required");
        }

        skill.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(skillRepository.save(skill));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Skill> updateSkill(@PathVariable Long id, @RequestBody Skill request) {
        return skillRepository.findById(id).map(skill -> {
            skill.setName(request.getName() == null ? "" : request.getName().trim());
            skill.setDescription(request.getDescription() == null ? "" : request.getDescription().trim());
            skill.setCategory(request.getCategory() == null || request.getCategory().isBlank() ? "Other" : request.getCategory().trim());
            skill.setLevel(request.getLevel() == null || request.getLevel().isBlank() ? "Beginner" : request.getLevel().trim());
            skill.setTeacherName(request.getTeacherName());
            skill.setTeacherId(request.getTeacherId());
            return ResponseEntity.ok(skillRepository.save(skill));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSkill(@PathVariable Long id) {
        if (!skillRepository.existsById(id)) return ResponseEntity.notFound().build();
        skillRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
