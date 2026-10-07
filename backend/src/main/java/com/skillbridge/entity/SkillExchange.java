package com.skillbridge.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "skill_exchanges")
public class SkillExchange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long requesterId;

    @Column(nullable = false)
    private Long recipientId;

    @Column(nullable = true)
    private Long offeredSkillId;

    @Column(nullable = true)
    private Long requestedSkillId;

    @Column(length = 150)
    private String offeredSkillName;

    @Column(length = 150)
    private String requestedSkillName;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public SkillExchange() {}

    @PrePersist
    public void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = "PENDING";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRequesterId() { return requesterId; }
    public void setRequesterId(Long requesterId) { this.requesterId = requesterId; }

    public Long getRecipientId() { return recipientId; }
    public void setRecipientId(Long recipientId) { this.recipientId = recipientId; }

    public Long getOfferedSkillId() { return offeredSkillId; }
    public void setOfferedSkillId(Long offeredSkillId) { this.offeredSkillId = offeredSkillId; }

    public Long getRequestedSkillId() { return requestedSkillId; }
    public void setRequestedSkillId(Long requestedSkillId) { this.requestedSkillId = requestedSkillId; }

    public String getOfferedSkillName() { return offeredSkillName; }
    public void setOfferedSkillName(String offeredSkillName) { this.offeredSkillName = offeredSkillName; }

    public String getRequestedSkillName() { return requestedSkillName; }
    public void setRequestedSkillName(String requestedSkillName) { this.requestedSkillName = requestedSkillName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
