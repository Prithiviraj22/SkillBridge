package com.skillbridge.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "learning_plans")
public class LearningPlan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long studentId;
    private Long mentorId;
    @Column(nullable=false) private Long skillId;
    @Column(nullable=false) private String title;
    @Column(length=2000) private String description;
    private LocalDate targetDate;
    @Column(nullable=false) private Integer progress=0;
    @Column(nullable=false) private String status="IN_PROGRESS";
    @Column(columnDefinition="LONGTEXT") private String segmentsJson="[]";

    public LearningPlan() {}
    @PrePersist @PreUpdate public void normalize(){
        if(progress==null) progress=0; if(progress<0) progress=0; if(progress>100) progress=100;
        status=progress>=100?"COMPLETED":"IN_PROGRESS"; if(segmentsJson==null)segmentsJson="[]";
    }
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Long getStudentId(){return studentId;} public void setStudentId(Long v){studentId=v;}
    public Long getMentorId(){return mentorId;} public void setMentorId(Long v){mentorId=v;}
    public Long getSkillId(){return skillId;} public void setSkillId(Long v){skillId=v;}
    public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public LocalDate getTargetDate(){return targetDate;} public void setTargetDate(LocalDate v){targetDate=v;}
    public Integer getProgress(){return progress;} public void setProgress(Integer v){progress=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public String getSegmentsJson(){return segmentsJson;} public void setSegmentsJson(String v){segmentsJson=v;}
}
