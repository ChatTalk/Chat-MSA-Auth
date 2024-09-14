package com.example.chatserverauth.domain.dto;

import com.example.chatserverauth.domain.entity.UserRoleEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class UserInfoDTO {
    private String email;
    private UserRoleEnum role;
}
