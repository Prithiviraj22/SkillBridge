package com.skillbridge.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillbridge.entity.LearningPlan;
import com.skillbridge.entity.Skill;
import com.skillbridge.repository.LearningPlanRepository;
import com.skillbridge.repository.SkillRepository;
import com.skillbridge.repository.UserRepository;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;

@RestController @RequestMapping("/api/learning-plans")
public class LearningPlanController {
    private final LearningPlanRepository plans; private final SkillRepository skills; private final UserRepository users; private final ObjectMapper mapper;
    public LearningPlanController(LearningPlanRepository plans, SkillRepository skills, UserRepository users, ObjectMapper mapper){this.plans=plans;this.skills=skills;this.users=users;this.mapper=mapper;}

    @GetMapping("/student/{id}") public List<Map<String,Object>> student(@PathVariable Long id){return plans.findByStudentIdOrderByIdDesc(id).stream().map(this::response).toList();}
    @GetMapping("/mentor/{id}") public List<Map<String,Object>> mentor(@PathVariable Long id){return plans.findByMentorIdOrderByIdDesc(id).stream().map(this::response).toList();}

    @PostMapping public ResponseEntity<?> create(@RequestBody PlanRequest r){
        if(r.studentId==null||r.skillId==null)return bad("studentId and skillId are required");
        Skill s=skills.findById(r.skillId).orElse(null); if(s==null)return ResponseEntity.status(404).body(Map.of("message","Skill not found"));
        LearningPlan p=new LearningPlan(); p.setStudentId(r.studentId);p.setMentorId(r.mentorId!=null?r.mentorId:s.getTeacherId());p.setSkillId(r.skillId);
        p.setTitle(blank(r.title)?"Learn "+s.getName():r.title.trim());p.setDescription(blank(r.description)?"Structured plan for "+s.getName():r.description);p.setTargetDate(r.targetDate);
        p.setSegmentsJson(toJson(r.segments!=null&&!r.segments.isEmpty()?r.segments:template(s.getName(),s.getLevel()))); return ResponseEntity.status(201).body(response(plans.save(p)));
    }

    @PostMapping("/auto") public ResponseEntity<?> auto(@RequestBody AutoRequest r){
        if(r.studentId==null||r.skillId==null)return bad("studentId and skillId are required");
        Skill s=skills.findById(r.skillId).orElse(null); if(s==null)return ResponseEntity.status(404).body(Map.of("message","Skill not found"));
        Optional<LearningPlan> existing=plans.findByStudentIdAndSkillId(r.studentId,r.skillId); if(existing.isPresent())return ResponseEntity.ok(response(existing.get()));
        LearningPlan p=new LearningPlan();p.setStudentId(r.studentId);p.setMentorId(s.getTeacherId());p.setSkillId(s.getId());p.setTitle(s.getName()+" Learning Plan");p.setDescription("Automatically generated structured plan for "+s.getName()+". Learner and mentor can customize it.");p.setSegmentsJson(toJson(template(s.getName(),s.getLevel())));return ResponseEntity.status(201).body(response(plans.save(p)));
    }

    @PutMapping("/{id}") public ResponseEntity<?> update(@PathVariable Long id,@RequestBody UpdateRequest r){
        return plans.findById(id).map(p->{ if(!allowed(p,r.userId))return ResponseEntity.status(403).body(Map.of("message","You cannot modify this plan"));
            if(r.title!=null)p.setTitle(r.title);if(r.description!=null)p.setDescription(r.description);if(r.targetDate!=null)p.setTargetDate(r.targetDate);if(r.segments!=null)p.setSegmentsJson(toJson(r.segments));
            recalc(p);return ResponseEntity.ok(response(plans.save(p)));}).orElse(ResponseEntity.notFound().build());
    }
    @PutMapping("/{id}/segment/{index}") public ResponseEntity<?> segment(@PathVariable Long id,@PathVariable int index,@RequestBody SegmentRequest r){
        return plans.findById(id).map(p->{if(!allowed(p,r.userId))return ResponseEntity.status(403).body(Map.of("message","You cannot modify this plan"));
            try{List<Segment> list=mapper.readValue(p.getSegmentsJson(),new TypeReference<>(){});if(index<0||index>=list.size())return ResponseEntity.badRequest().body(Map.of("message","Invalid segment"));list.get(index).completed=Boolean.TRUE.equals(r.completed);p.setSegmentsJson(toJson(list));recalc(p);return ResponseEntity.ok(response(plans.save(p)));}catch(Exception e){return ResponseEntity.internalServerError().body(Map.of("message","Could not update segment"));}}).orElse(ResponseEntity.notFound().build());
    }
    @DeleteMapping("/{id}") public ResponseEntity<?> delete(@PathVariable Long id,@RequestParam Long userId){return plans.findById(id).map(p->{if(!allowed(p,userId))return ResponseEntity.status(403).build();plans.delete(p);return ResponseEntity.noContent().build();}).orElse(ResponseEntity.notFound().build());}

    private boolean allowed(LearningPlan p,Long u){return u!=null&&(u.equals(p.getStudentId())||u.equals(p.getMentorId()));}
    private void recalc(LearningPlan p){try{List<Segment> l=mapper.readValue(p.getSegmentsJson(),new TypeReference<>(){});int done=(int)l.stream().filter(x->x.completed).count();p.setProgress(l.isEmpty()?0:Math.round(done*100f/l.size()));p.setStatus(p.getProgress()>=100?"COMPLETED":"IN_PROGRESS");}catch(Exception ignored){}}
    private Map<String,Object> response(LearningPlan p){Map<String,Object> m=new LinkedHashMap<>();m.put("id",p.getId());m.put("studentId",p.getStudentId());m.put("mentorId",p.getMentorId());m.put("skillId",p.getSkillId());m.put("title",p.getTitle());m.put("description",p.getDescription());m.put("targetDate",p.getTargetDate());m.put("progress",p.getProgress());m.put("status",p.getStatus());try{m.put("segments",mapper.readValue(p.getSegmentsJson(),new TypeReference<List<Segment>>(){}));}catch(Exception e){m.put("segments",List.of());}skills.findById(p.getSkillId()).ifPresent(s->{m.put("skillName",s.getName());m.put("category",s.getCategory());m.put("level",s.getLevel());m.put("mentorName",s.getTeacherName());});return m;}
    private String toJson(Object o){try{return mapper.writeValueAsString(o);}catch(Exception e){return "[]";}}
    private boolean blank(String s){return s==null||s.isBlank();}
    private ResponseEntity<?> bad(String s){return ResponseEntity.badRequest().body(Map.of("message",s));}
    private List<Segment> template(String name,String level){String n=name.toLowerCase();List<String> t=new ArrayList<>();if(n.contains("java"))t=List.of("Java fundamentals","Control statements and methods","Arrays and Strings","Object-Oriented Programming","Exception handling","Collections Framework","Multithreading","JDBC and MySQL","Build a Java mini project");else if(n.contains("react"))t=List.of("JSX and components","Props and component design","State and events","useEffect and hooks","Forms and validation","React Router","API integration","State management","Build a React project");else if(n.contains("python"))t=List.of("Python syntax and variables","Conditions and loops","Functions","Lists, tuples and dictionaries","OOP in Python","Modules and packages","File handling and exceptions","Libraries and APIs","Build a Python project");else if(n.contains("javascript"))t=List.of("Syntax and variables","Functions and scope","Arrays and objects","DOM manipulation","ES6 features","Promises and async/await","APIs and fetch","Build a JavaScript project");else if(n.contains("sql")||n.contains("mysql"))t=List.of("Database fundamentals","CREATE and ALTER","INSERT, UPDATE and DELETE","SELECT and filtering","Joins","Subqueries and aggregation","Views and constraints","Build a database project");else t=List.of("Understand fundamentals","Learn core concepts","Practice guided exercises","Complete intermediate tasks","Apply advanced concepts","Build a practical mini project","Review and improve project","Complete final assessment");List<Segment> out=new ArrayList<>();for(int i=0;i<t.size();i++){Segment s=new Segment();s.title=t.get(i);s.description="Complete this stage of the "+name+" learning path.";s.completed=false;out.add(s);}return out;}
    public static class Segment{public String title;public String description;public boolean completed;public Segment(){} }
    public static class PlanRequest{public Long studentId,skillId,mentorId;public String title,description;public LocalDate targetDate;public List<Segment> segments;}
    public static class AutoRequest{public Long studentId,skillId;}
    public static class UpdateRequest{public Long userId;public String title,description;public LocalDate targetDate;public List<Segment> segments;}
    public static class SegmentRequest{public Long userId;public Boolean completed;}
}
