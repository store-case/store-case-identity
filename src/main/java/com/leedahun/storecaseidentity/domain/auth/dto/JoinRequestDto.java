package com.leedahun.storecaseidentity.domain.auth.dto;

import com.leedahun.storecaseidentity.domain.auth.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class JoinRequestDto {
    private String email;
    private String password;
    private String name;
    private String phone;

    public User toEntity(String encodedPassword) {
        return User.builder()
                .email(this.email)
                .password(encodedPassword)
                .name(this.name)
                .phone(this.phone)
                .build();
    }

}
