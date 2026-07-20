package com.stylemind.order.entity;

import com.stylemind.common.util.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "orders")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends BaseEntity {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;

    @Column(name = "total_amount", precision = 12, scale = 2, nullable = false)
    private java.math.BigDecimal totalAmount;

    @Column(name = "order_status", length = 30, nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private OrderStatus orderStatus = OrderStatus.PENDING;

    @Column(name = "shipping_address", columnDefinition = "TEXT", nullable = false)
    private String shippingAddress;

    @Column(name = "source_address_id", length = 50)
    private String sourceAddressId;

    @Column(name = "shipping_recipient_name", length = 100)
    private String shippingRecipientName;

    @Column(name = "shipping_phone", length = 20)
    private String shippingPhone;

    @Column(name = "shipping_province_code", length = 10)
    private String shippingProvinceCode;

    @Column(name = "shipping_province_name", length = 150)
    private String shippingProvinceName;

    @Column(name = "shipping_ward_code", length = 10)
    private String shippingWardCode;

    @Column(name = "shipping_ward_name", length = 150)
    private String shippingWardName;

    @Column(name = "shipping_address_line", columnDefinition = "TEXT")
    private String shippingAddressLine;

    @Column(name = "shipping_note", columnDefinition = "TEXT")
    private String shippingNote;

    // Explicit getters/setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public java.math.BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(java.math.BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    // Mutating this directly bypasses OrderStatusService.changeStatus() and its
    // transition validation/audit trail. Only OrderStatusService should call this.
    public OrderStatus getOrderStatus() { return orderStatus; }
    public void setOrderStatus(OrderStatus orderStatus) { this.orderStatus = orderStatus; }
    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
}
