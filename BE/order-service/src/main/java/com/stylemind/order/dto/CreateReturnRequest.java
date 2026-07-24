package com.stylemind.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReturnRequest {

    @NotNull(message = "REASON_REQUIRED")
    private String reason; // DEFECTIVE, DAMAGED, WRONG_ITEM, MISSING_ITEM, SIZE_NOT_FIT, CHANGED_MIND, OTHER

    private String customerNote;

    @NotEmpty(message = "ITEMS_REQUIRED")
    @Valid
    private List<ReturnItemRequest> items;

    @Valid
    private List<EvidenceRequest> evidences;
}
