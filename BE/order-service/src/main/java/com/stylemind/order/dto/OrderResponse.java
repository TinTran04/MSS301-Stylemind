package com.stylemind.order.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {
    private String id;
    private String userId;
    private String customerEmail;
    private BigDecimal totalAmount;
    private String orderStatus;
    private List<String> availableTransitions;
    private String paymentTransactionId;
    private String paymentStatus;
    private String qrContent;
    private String qrImageUrl;
    private String transferContent;
    private Instant paymentExpiresAt;
    private String paymentMethod;
    private String paymentReference;
    private String gatewayTransactionId;
    private Instant paidAt;
    private String shippingAddress;
    private String sourceAddressId;
    private String shippingRecipientName;
    private String shippingPhone;
    private String shippingProvinceCode;
    private String shippingProvinceName;
    private String shippingWardCode;
    private String shippingWardName;
    private String shippingAddressLine;
    private String shippingNote;
    private List<OrderItemResponse> items;
    private List<OrderStatusHistoryResponse> statusHistory;
    private Instant createdAt;
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }
    public List<String> getAvailableTransitions() { return availableTransitions; }
    public void setAvailableTransitions(List<String> availableTransitions) { this.availableTransitions = availableTransitions; }
    public String getPaymentTransactionId() { return paymentTransactionId; }
    public void setPaymentTransactionId(String paymentTransactionId) { this.paymentTransactionId = paymentTransactionId; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getQrContent() { return qrContent; }
    public void setQrContent(String qrContent) { this.qrContent = qrContent; }
    public String getQrImageUrl() { return qrImageUrl; }
    public void setQrImageUrl(String qrImageUrl) { this.qrImageUrl = qrImageUrl; }
    public String getTransferContent() { return transferContent; }
    public void setTransferContent(String transferContent) { this.transferContent = transferContent; }
    public Instant getPaymentExpiresAt() { return paymentExpiresAt; }
    public void setPaymentExpiresAt(Instant paymentExpiresAt) { this.paymentExpiresAt = paymentExpiresAt; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }
    public String getGatewayTransactionId() { return gatewayTransactionId; }
    public void setGatewayTransactionId(String gatewayTransactionId) { this.gatewayTransactionId = gatewayTransactionId; }
    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }
    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
    public String getSourceAddressId() { return sourceAddressId; }
    public void setSourceAddressId(String sourceAddressId) { this.sourceAddressId = sourceAddressId; }
    public String getShippingRecipientName() { return shippingRecipientName; }
    public void setShippingRecipientName(String shippingRecipientName) { this.shippingRecipientName = shippingRecipientName; }
    public String getShippingPhone() { return shippingPhone; }
    public void setShippingPhone(String shippingPhone) { this.shippingPhone = shippingPhone; }
    public String getShippingProvinceCode() { return shippingProvinceCode; }
    public void setShippingProvinceCode(String shippingProvinceCode) { this.shippingProvinceCode = shippingProvinceCode; }
    public String getShippingProvinceName() { return shippingProvinceName; }
    public void setShippingProvinceName(String shippingProvinceName) { this.shippingProvinceName = shippingProvinceName; }
    public String getShippingWardCode() { return shippingWardCode; }
    public void setShippingWardCode(String shippingWardCode) { this.shippingWardCode = shippingWardCode; }
    public String getShippingWardName() { return shippingWardName; }
    public void setShippingWardName(String shippingWardName) { this.shippingWardName = shippingWardName; }
    public String getShippingAddressLine() { return shippingAddressLine; }
    public void setShippingAddressLine(String shippingAddressLine) { this.shippingAddressLine = shippingAddressLine; }
    public String getShippingNote() { return shippingNote; }
    public void setShippingNote(String shippingNote) { this.shippingNote = shippingNote; }
    public List<OrderItemResponse> getItems() { return items; }
    public void setItems(List<OrderItemResponse> items) { this.items = items; }
    public List<OrderStatusHistoryResponse> getStatusHistory() { return statusHistory; }
    public void setStatusHistory(List<OrderStatusHistoryResponse> statusHistory) { this.statusHistory = statusHistory; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
