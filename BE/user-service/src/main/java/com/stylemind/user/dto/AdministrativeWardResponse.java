package com.stylemind.user.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AdministrativeWardResponse {
    String code;
    String provinceCode;
    String name;
    String type;
}
