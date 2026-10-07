package com.skillbridge.controller;

import com.skillbridge.entity.Skill;
import com.skillbridge.entity.User;
import com.skillbridge.repository.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final UserRepository users; private final SkillRepository skills; private final MentorRequestRepository requests;
    private final LearningPlanRepository plans; private final SkillExchangeRepository exchanges;

    public AdminController(UserRepository users, SkillRepository skills, MentorRequestRepository requests,
                           LearningPlanRepository plans, SkillExchangeRepository exchanges){
        this.users=users;this.skills=skills;this.requests=requests;this.plans=plans;this.exchanges=exchanges;
    }

    @GetMapping("/stats")
    public Map<String,Object> stats(){
        long students=users.findAll().stream().filter(u->"STUDENT".equalsIgnoreCase(u.getRole())).count();
        long mentors=users.findAll().stream().filter(u->"TEACHER".equalsIgnoreCase(u.getRole())).count();
        Map<String,Object> m=new LinkedHashMap<>();
        m.put("users",users.count());m.put("students",students);m.put("mentors",mentors);m.put("skills",skills.count());
        m.put("requests",requests.count());m.put("learningPlans",plans.count());m.put("exchanges",exchanges.count());
        return m;
    }

    @GetMapping("/users") public List<User> users(){return users.findAll();}
    @DeleteMapping("/users/{id}") public ResponseEntity<?> deleteUser(@PathVariable Long id){
        return users.findById(id).map(u->{if("ADMIN".equalsIgnoreCase(u.getRole())) return ResponseEntity.badRequest().body(Map.of("message","Admin account cannot be deleted"));users.delete(u);return ResponseEntity.noContent().build();}).orElse(ResponseEntity.notFound().build());
    }
    @PutMapping("/users/{id}/role") public ResponseEntity<?> role(@PathVariable Long id,@RequestBody Map<String,String> body){
        return users.findById(id).map(u->{String role=body.get("role");if(role==null||!Set.of("STUDENT","TEACHER","ADMIN").contains(role.toUpperCase()))return ResponseEntity.badRequest().body(Map.of("message","Invalid role"));u.setRole(role.toUpperCase());return ResponseEntity.ok(users.save(u));}).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/skills") public List<Skill> skills(){return skills.findAll();}
    @DeleteMapping("/skills/{id}") public ResponseEntity<Void> deleteSkill(@PathVariable Long id){if(!skills.existsById(id))return ResponseEntity.notFound().build();skills.deleteById(id);return ResponseEntity.noContent().build();}

    @GetMapping("/requests") public List<Map<String,Object>> requestData(){
        return requests.findAll().stream().map(r->{Map<String,Object> m=new LinkedHashMap<>();m.put("id",r.getId());m.put("status",r.getStatus());m.put("scheduledAt",r.getScheduledAt());m.put("createdAt",r.getCreatedAt());m.put("studentId",r.getStudentId());m.put("teacherId",r.getTeacherId());m.put("skillId",r.getSkillId());return m;}).toList();
    }
}
