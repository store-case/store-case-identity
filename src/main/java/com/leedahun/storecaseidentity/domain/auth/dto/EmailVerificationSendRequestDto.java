package com.leedahun.storecaseidentity.domain.auth.dto;

import lombok.*;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmailVerificationSendRequestDto {
    private String email;
}
