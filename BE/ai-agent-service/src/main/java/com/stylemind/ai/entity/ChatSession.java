package com.stylemind.ai.entity;

import com.stylemind.common.util.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "chat_sessions")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatSession extends BaseEntity {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @Column(name = "user_id", length = 50)
    private String userId;

    @Column(name = "context_weather_temp", precision = 4, scale = 1)
    private java.math.BigDecimal contextWeatherTemp;

    @Column(name = "context_weather_condition", length = 30)
    private String contextWeatherCondition;
}