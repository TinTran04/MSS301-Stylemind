package com.stylemind.user.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AdministrativeProvinceResponse {
    String code;
    String name;
    String type;
}
