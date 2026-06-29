package com.stylemind.user.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminUserPageResponse {
    private List<UserProfileResponse> items;
    private int page;
    private int size;
    private long totalItems;
    private int totalPages;
}
