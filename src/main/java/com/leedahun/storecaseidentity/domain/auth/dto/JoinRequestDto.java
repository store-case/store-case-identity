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

    public User toEntity() {
        return User.builder()
                .email(this.email)
                .password(this.password)
                .name(this.name)
                .phone(this.phone)
                .build();
    }

    public void setEncodedPassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}
