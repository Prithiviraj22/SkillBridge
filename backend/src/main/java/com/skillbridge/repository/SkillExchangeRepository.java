package com.skillbridge.repository;
import com.skillbridge.entity.SkillExchange;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SkillExchangeRepository extends JpaRepository<SkillExchange,Long>{
    List<SkillExchange> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);
    List<SkillExchange> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);
}
