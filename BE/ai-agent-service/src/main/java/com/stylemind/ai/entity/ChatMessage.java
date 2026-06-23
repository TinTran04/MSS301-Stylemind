package com.stylemind.ai.entity;

import com.stylemind.common.util.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chat_messages")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage extends BaseEntity {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "session_id", columnDefinition = "UUID", nullable = false)
    private java.util.UUID sessionId;

    @Column(name = "sender_type", length = 10, nullable = false)
    private String senderType; // USER, AI

    @Column(name = "message_text", columnDefinition = "TEXT", nullable = false)
    private String messageText;

    @Column(name = "has_product_block", nullable = false)
    @Builder.Default
    private Boolean hasProductBlock = false;
}