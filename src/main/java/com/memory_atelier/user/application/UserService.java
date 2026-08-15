package com.memory_atelier.user.application;


import com.memory_atelier.global.exception.CustomException;
import com.memory_atelier.global.exception.ErrorCode;
import com.memory_atelier.image.S3Uploader;
import com.memory_atelier.user.api.dto.request.UserSaveRequestDto;
import com.memory_atelier.user.api.dto.request.UserUpdateRequestDto;
import com.memory_atelier.user.api.dto.response.UserInfoResponseDto;
import com.memory_atelier.user.api.dto.response.UserListResponseDto;
import com.memory_atelier.user.domain.User;
import com.memory_atelier.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final S3Uploader s3Uploader;

    @Transactional
    public UserInfoResponseDto updateProfileImage(Long userId, MultipartFile file)  {
        User user = findActiveUserById(userId);

        String oldImageUrl = user.getProfileImageUrl();

        String newImageUrl;
        try {
            newImageUrl = s3Uploader.upload(file);
        } catch (IOException e) {
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        user.updateProfileImage(newImageUrl);

        if (oldImageUrl != null) {
            s3Uploader.delete(oldImageUrl);
        }

        return UserInfoResponseDto.from(user);
    }

    // 내 정보 조회
    public UserInfoResponseDto userFindMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.USER_NOT_FOUND,
                        ErrorCode.USER_NOT_FOUND.getMessage() + userId
                ));

        return UserInfoResponseDto.from(user);
    }

    // 회원가입
    @Transactional
    public Long signUp(UserSaveRequestDto requestDto) {
        if (userRepository.existsByEmail(requestDto.getEmail())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        User user = requestDto.toEntity(passwordEncoder);
        return userRepository.save(user).getUserId();
    }

    /*
     *단건 회원 조회
     * 탈퇴한 회원이거나 존재하지 않는 회원이면 동일하게 USER_NOT_FOUND(404) 반환
     * -> 탈퇴 여부를 클라이언트에게 노출하지 않기 위한 정책
     */
    public UserInfoResponseDto getUserInfo(Long userId) {
        User user = findActiveUserById(userId);
        return UserInfoResponseDto.from(user);
    }

    // 회원 목록 조회 (탈퇴 회원은 목록에서 제외)
    public UserListResponseDto getUserList(Pageable pageable) {
        Page<User> userPage = userRepository.findAllByDeletedAtIsNull(pageable);
        return UserListResponseDto.from(userPage);
    }

    // 회원 정보 수정
    @Transactional
    public UserInfoResponseDto updateUser(Long userId, UserUpdateRequestDto requestDto) {
        User user = findActiveUserById(userId);
        user.update(requestDto);
        return  UserInfoResponseDto.from(user);
    }

    // 회원 탈퇴
    @Transactional
    public void withdraw(Long userId) {
        User user = findActiveUserById(userId);
        user.withdraw();
    }

    private User findActiveUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!user.isActive()) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }
}
