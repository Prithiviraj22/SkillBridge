package com.skillbridge.controller;

import com.skillbridge.entity.StudentSkill;
import com.skillbridge.entity.User;
import com.skillbridge.repository.StudentSkillRepository;
import com.skillbridge.repository.UserRepository;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student-skills")
public class StudentSkillController {
    private final StudentSkillRepository skills;
    private final UserRepository users;

    public StudentSkillController(StudentSkillRepository skills, UserRepository users){
        this.skills=skills; this.users=users;
    }

    @GetMapping("/student/{studentId}")
    public List<StudentSkill> getMine(@PathVariable Long studentId){
        return skills.findByStudentIdOrderByIdDesc(studentId);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody StudentSkillRequest r){
        if(r.studentId==null || r.name==null || r.name.isBlank())
            return ResponseEntity.badRequest().body(Map.of("message","studentId and skill name are required"));
        User u=users.findById(r.studentId).orElse(null);
        if(u==null || !"STUDENT".equalsIgnoreCase(u.getRole()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message","Only student accounts can create student skills"));
        StudentSkill s=new StudentSkill();
        s.setStudentId(r.studentId); s.setName(r.name.trim());
        s.setCategory(blank(r.category)?"Other":r.category.trim());
        s.setLevel(blank(r.level)?"Beginner":r.level.trim());
        s.setDescription(blank(r.description)?"":r.description.trim());
        return ResponseEntity.status(HttpStatus.CREATED).body(skills.save(s));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,@RequestBody StudentSkillRequest r){
        return skills.findById(id).map(s->{
            if(r.studentId==null || !r.studentId.equals(s.getStudentId()))
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message","You can only edit your own skills"));
            if(r.name==null || r.name.isBlank())
                return ResponseEntity.badRequest().body(Map.of("message","Skill name is required"));
            s.setName(r.name.trim());
            s.setCategory(blank(r.category)?"Other":r.category.trim());
            s.setLevel(blank(r.level)?"Beginner":r.level.trim());
            s.setDescription(blank(r.description)?"":r.description.trim());
            return ResponseEntity.ok(skills.save(s));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id,@RequestParam Long studentId){
        return skills.findById(id).map(s->{
            if(!studentId.equals(s.getStudentId()))
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            skills.delete(s);
            return ResponseEntity.noContent().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    private boolean blank(String x){return x==null||x.isBlank();}

    public static class StudentSkillRequest{
        public Long studentId;
        public String name;
        public String category;
        public String level;
        public String description;
    }
}
