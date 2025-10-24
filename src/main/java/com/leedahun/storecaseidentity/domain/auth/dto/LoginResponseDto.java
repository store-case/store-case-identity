package com.leedahun.storecaseidentity.domain.auth.dto;

import com.leedahun.storecaseidentity.domain.auth.entity.Role;
import com.leedahun.storecaseidentity.domain.auth.entity.User;
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
    private Role role;
    private String accessToken;

    public static LoginResponseDto from(User user, String accessToken) {
        return LoginResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .accessToken(accessToken)
                .build();
    }
}
