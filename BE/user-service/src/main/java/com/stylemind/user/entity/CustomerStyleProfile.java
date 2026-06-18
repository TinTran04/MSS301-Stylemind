package com.stylemind.user.entity;

import com.stylemind.common.util.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "customer_style_profiles")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerStyleProfile extends BaseEntity {

    @Id
    @Column(name = "user_id", length = 50)
    private String userId;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "age")
    private Integer age;

    @Column(name = "height_cm", precision = 5, scale = 2)
    private java.math.BigDecimal heightCm;

    @Column(name = "weight_kg", precision = 5, scale = 2)
    private java.math.BigDecimal weightKg;

    @Column(name = "body_morphology", length = 50)
    private String bodyMorphology;

    @Column(name = "preferred_fit", length = 30)
    private String preferredFit;

    @Column(name = "style_personas", columnDefinition = "jsonb")
    private String stylePersonas;
}