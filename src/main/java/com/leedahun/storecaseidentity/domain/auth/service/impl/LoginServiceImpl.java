package com.leedahun.storecaseidentity.domain.auth.service.impl;

import com.leedahun.storecaseidentity.common.error.exception.EntityNotFoundException;
import com.leedahun.storecaseidentity.domain.auth.dto.*;
import com.leedahun.storecaseidentity.domain.auth.entity.Role;
import com.leedahun.storecaseidentity.domain.auth.entity.User;
import com.leedahun.storecaseidentity.domain.auth.exception.InvalidPasswordException;
import com.leedahun.storecaseidentity.domain.auth.exception.UserAlreadyExistsException;
import com.leedahun.storecaseidentity.domain.auth.repository.UserRepository;
import com.leedahun.storecaseidentity.domain.auth.service.LoginService;
import com.leedahun.storecaseidentity.domain.auth.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {

    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @Override
    public void join(JoinRequestDto joinRequestDto) {
        userRepository.findByEmail(joinRequestDto.getEmail())
                .ifPresent(u -> {throw new UserAlreadyExistsException();});

        joinRequestDto.setEncodedPassword(passwordEncoder.encode(joinRequestDto.getPassword()));
        User user = joinRequestDto.toEntity();
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResult login(LoginRequestDto loginRequestDto) {
        User user = userRepository.findByEmail(loginRequestDto.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("User", loginRequestDto.getEmail()));

        if (!passwordEncoder.matches(loginRequestDto.getPassword(), user.getPassword())) {
            throw new InvalidPasswordException();
        }

        TokenResult tokens = issueTokens(user.getId(), user.getRole());
        LoginResponseDto loginResponse = LoginResponseDto.from(user, tokens.getAccessToken());
        return LoginResult.from(loginResponse, tokens.getRefreshToken());
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResult reissueTokens(String refreshToken) {
        LoginUser loginUser = jwtUtil.verify(refreshToken);

        User user = userRepository.findById(loginUser.getId())
                .orElseThrow(() -> new EntityNotFoundException("User", String.valueOf(loginUser.getId())));

        return issueTokens(user.getId(), user.getRole());
    }

    private TokenResult issueTokens(Long userId, Role role) {
        return TokenResult.builder()
                .accessToken(jwtUtil.createAccessToken(userId, role))
                .refreshToken(jwtUtil.createRefreshToken(userId, role))
                .build();
    }

}