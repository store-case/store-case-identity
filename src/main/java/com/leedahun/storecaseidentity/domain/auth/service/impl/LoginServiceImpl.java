package com.leedahun.storecaseidentity.domain.auth.service.impl;

import com.leedahun.storecaseidentity.common.error.exception.EntityNotFoundException;
import com.leedahun.storecaseidentity.domain.auth.constant.JwtConstants;
import com.leedahun.storecaseidentity.domain.auth.dto.JoinRequestDto;
import com.leedahun.storecaseidentity.domain.auth.dto.LoginRequestDto;
import com.leedahun.storecaseidentity.domain.auth.dto.LoginUser;
import com.leedahun.storecaseidentity.domain.auth.dto.TokenResponseDto;
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

        User user = User.builder()
                .email(joinRequestDto.getEmail())
                .password(passwordEncoder.encode(joinRequestDto.getPassword()))
                .name(joinRequestDto.getName())
                .phone(joinRequestDto.getPhone())
                .build();

        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponseDto login(LoginRequestDto loginRequestDto) {
        User user = userRepository.findByEmail(loginRequestDto.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("User", loginRequestDto.getEmail()));

        if (!passwordEncoder.matches(loginRequestDto.getPassword(), user.getPassword())) {
            throw new InvalidPasswordException();
        }

        Long id = user.getId();
        Role role = user.getRole();
        return TokenResponseDto.builder()
                .accessToken(JwtConstants.TOKEN_PREFIX + jwtUtil.createAccessToken(id, role))
                .refreshToken(JwtConstants.TOKEN_PREFIX + jwtUtil.createRefreshToken(id, role))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponseDto reissueTokens(String refreshToken) {
        LoginUser loginUser = jwtUtil.verify(refreshToken);

        User user = userRepository.findById(loginUser.getId())
                .orElseThrow(() -> new EntityNotFoundException("User", String.valueOf(loginUser.getId())));

        return TokenResponseDto.builder()
                .accessToken(JwtConstants.TOKEN_PREFIX + jwtUtil.createAccessToken(user.getId(), user.getRole()))
                .refreshToken(JwtConstants.TOKEN_PREFIX + jwtUtil.createRefreshToken(user.getId(), user.getRole()))
                .build();
    }

}