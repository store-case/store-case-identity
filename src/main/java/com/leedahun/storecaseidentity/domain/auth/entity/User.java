package com.leedahun.storecaseidentity.domain.auth.entity;

import com.leedahun.storecaseidentity.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "`user`")
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 120)
    private String email;

    private String password;

    @Column(length = 60)
    private String name;

    @Column(length = 30)
    private String phone;

    @Builder.Default
    private boolean isWithdraw = false;

    @Builder.Default
    private boolean isSocial = false;

    private String snsType;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;

}
