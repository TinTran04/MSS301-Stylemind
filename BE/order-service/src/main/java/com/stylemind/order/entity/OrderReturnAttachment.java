package com.stylemind.order.entity;

import com.stylemind.common.util.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "order_return_attachments")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderReturnAttachment extends BaseEntity {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "return_request_id", length = 50, nullable = false)
    private String returnRequestId;

    @Column(name = "order_id", length = 50, nullable = false)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner", length = 20, nullable = false)
    private ReturnAttachmentOwner owner;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", length = 40, nullable = false)
    private ReturnAttachmentKind kind;

    @Column(name = "file_name", length = 255, nullable = false)
    private String fileName;

    @Column(name = "content_type", length = 100, nullable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Basic(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "image_data", nullable = false)
    private byte[] imageData;
}
