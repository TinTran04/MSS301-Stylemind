package com.stylemind.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "administrative_provinces")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdministrativeProvince {
    @Id
    @Column(name = "code", length = 10)
    private String code;

    @Column(name = "name", length = 150, nullable = false)
    private String name;

    @Column(name = "type", length = 30, nullable = false)
    private String type;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "data_version", length = 50, nullable = false)
    private String dataVersion;
}
