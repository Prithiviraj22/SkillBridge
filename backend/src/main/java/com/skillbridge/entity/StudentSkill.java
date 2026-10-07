package com.skillbridge.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "student_skills")
public class StudentSkill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long studentId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String level;

    @Column(length = 1000)
    private String description;

    public StudentSkill() {}

    public Long getId(){ return id; }
    public void setId(Long id){ this.id=id; }
    public Long getStudentId(){ return studentId; }
    public void setStudentId(Long studentId){ this.studentId=studentId; }
    public String getName(){ return name; }
    public void setName(String name){ this.name=name; }
    public String getCategory(){ return category; }
    public void setCategory(String category){ this.category=category; }
    public String getLevel(){ return level; }
    public void setLevel(String level){ this.level=level; }
    public String getDescription(){ return description; }
    public void setDescription(String description){ this.description=description; }
}
