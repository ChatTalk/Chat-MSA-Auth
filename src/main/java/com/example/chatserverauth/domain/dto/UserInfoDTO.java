package com.example.chatserverauth.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class UserInfoDTO {
    private UUID id;
    private String email;
    private String role;
}
