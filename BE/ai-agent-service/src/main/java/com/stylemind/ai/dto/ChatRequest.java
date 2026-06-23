package com.stylemind.ai.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRequest {
    @NotBlank(message = "Tin nhắn không được để trống")
    @Size(max = 2000, message = "Tin nhắn tối đa 2000 ký tự")
    private String message;

    private UUID conversationId;
}
