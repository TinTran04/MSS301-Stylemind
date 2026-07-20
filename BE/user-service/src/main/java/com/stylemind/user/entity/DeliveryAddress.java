package com.stylemind.user.entity;

import com.stylemind.common.util.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "delivery_addresses")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryAddress extends BaseEntity {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;

    @Column(name = "recipient_name", length = 100, nullable = false)
    private String recipientName;

    @Column(name = "phone_number", length = 20, nullable = false)
    private String phoneNumber;

    @Column(name = "address_line", columnDefinition = "TEXT", nullable = false)
    private String addressLine;

    @Column(name = "city", length = 100, nullable = false)
    @Deprecated
    private String city;

    @Column(name = "province_code", length = 10)
    private String provinceCode;

    @Column(name = "province_name", length = 150)
    private String provinceName;

    @Column(name = "ward_code", length = 10)
    private String wardCode;

    @Column(name = "ward_name", length = 150)
    private String wardName;

    @Column(name = "shipping_note", columnDefinition = "TEXT")
    private String shippingNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status", length = 30, nullable = false)
    @Builder.Default
    private AddressValidationStatus validationStatus = AddressValidationStatus.LEGACY_UNVERIFIED;

    @Column(name = "administrative_data_version", length = 50)
    private String administrativeDataVersion;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;
}
