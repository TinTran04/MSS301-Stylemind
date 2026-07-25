package com.stylemind.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminQcRequest {

    @NotNull(message = "QC_PASSED_REQUIRED")
    private Boolean qcPassed;

    private String adminNote;
}
