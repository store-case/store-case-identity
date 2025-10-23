package com.leedahun.storecaseidentity.domain.auth.dto;

import com.leedahun.storecaseidentity.domain.auth.entity.Role;
import lombok.*;

@Setter
@Getter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponseDto {
    private Long id;
    private String email;
    private String name;
    private String accessToken;
    private Role role;
}
