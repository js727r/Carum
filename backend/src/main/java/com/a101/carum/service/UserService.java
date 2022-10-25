package com.a101.carum.service;

import com.a101.carum.api.dto.*;
import com.a101.carum.common.exception.UnAuthorizedException;
import com.a101.carum.domain.user.User;
import com.a101.carum.domain.user.UserDetail;
import com.a101.carum.repository.UserDetailRepository;
import com.a101.carum.repository.UserRepository;
import com.a101.carum.util.EncryptUtils;
import com.a101.carum.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLIntegrityConstraintViolationException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserDetailRepository userDetailRepository;
    private final JwtService jwtService;
    private final EncryptUtils encryptUtils;
    private final StringUtils stringUtils;

    @Transactional
    public void createUser(ReqPostUser reqPostUser) throws NoSuchAlgorithmException {

        String password = encryptPassword(reqPostUser.getUserId(), reqPostUser.getPassword());

        User user = User.builder()
                .userId(reqPostUser.getUserId())
                .birth(reqPostUser.getBirth())
                .nickName(reqPostUser.getNickName())
                .password(password)
                .phone(reqPostUser.getPhone())
                .build();

        user = userRepository.save(user);

        // TODO: Room 4개 생성 및 Main Room 설정해서 코드 수정

        UserDetail userDetail = UserDetail.builder()
                .user(user)
                .build();

        userDetailRepository.save(userDetail);
    }

    public ResLoginUser loginUser(ReqLoginUser reqLoginUser) throws UnsupportedEncodingException, NoSuchAlgorithmException {

        String password = encryptPassword(reqLoginUser.getUserId(), reqLoginUser.getPassword());

        User user = userRepository.findByUserIdAndPasswordAndIsDeleted(
                reqLoginUser.getUserId(),
                password,
                false).orElseThrow(() -> new NullPointerException("User를 찾을 수 없습니다."));

        String accessToken = jwtService.createToken(user);
        String refreshToken = jwtService.createRefreshToken();

        // TODO: refresh token redis에 저장

        return ResLoginUser.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public void logoutUser(Long id) {
        // TODO: delete refreshtoken from redis
    }

    @Transactional
    public ResGetUser readUser(Long id) {
        ResGetUser.ResGetUserBuilder resGetUserBuilder = ResGetUser.builder();

        // User Table에서 정보 가져오기
        User user = userRepository.findByIdAndIsDeleted(id, false).orElseThrow(() -> new NullPointerException("User를 찾을 수 없습니다."));
        resGetUserBuilder
                .userId(user.getUserId())
                .nickName(user.getNickName())
                .birth(user.getBirth())
                .phone(user.getPhone());

        // User Detail에서 정보 가져오기
        UserDetail userDetail = userDetailRepository.findByUser(user).orElseThrow(() -> new NullPointerException("User 정보가 손상되었습니다."));
        resGetUserBuilder
                .money(userDetail.getMoney());

        // TODO: Main Room 관련 정보 삽입

        return resGetUserBuilder.build();
    }

    public void readUserId(ReqGetUserId reqGetUserId) throws SQLIntegrityConstraintViolationException {
        User user = userRepository.findByUserIdAndIsDeleted(reqGetUserId.getUserId(), false);
        if (user != null) {
            throw new SQLIntegrityConstraintViolationException("아이디 중복입니다.");
        }
    }

    public void readNickName(ReqGetNickName reqGetNickName) throws SQLIntegrityConstraintViolationException {
        User user = userRepository.findByNickNameAndIsDeleted(reqGetNickName.getNickName(), false);
        if (user != null) {
            throw new SQLIntegrityConstraintViolationException("닉네임 중복입니다.");
        }
    }

    @Transactional
    public void updateUser(ReqPatchUser reqPatchUser, Long id) {
        User user = userRepository.findByIdAndIsDeleted(id, false).orElseThrow(() -> new NullPointerException("User를 찾을 수 없습니다."));
        user.updateNickName(reqPatchUser.getNickName());
    }

    @Transactional
    public void updateUserPassword(ReqPatchUserPassword reqPatchUserPassword, Long id) throws NoSuchAlgorithmException {
        User user = userRepository.findByIdAndIsDeleted(id, false).orElseThrow(() -> new NullPointerException("User를 찾을 수 없습니다."));

        String oldPassword = encryptPassword(user.getUserId(), reqPatchUserPassword.getOldPassword());

        // TODO: 비밀번호 암호화해서 체크하는 과정 추가
        if(!user.getPassword().equals(oldPassword)) {
            throw new UnAuthorizedException("이전 비밀번호를 똑바로 입력해주시길 바랍니다.");
        }

        String newPassword = encryptPassword(user.getUserId(), reqPatchUserPassword.getNewPassword());
        user.updatePassword(newPassword);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findByIdAndIsDeleted(id, false)
                .orElseThrow(() -> new NullPointerException("User를 찾을 수 없습니다."));
        user.updateIsDeleted();
    }

    public String encryptPassword(String userId, String password) throws NoSuchAlgorithmException {
        StringBuilder sb = new StringBuilder();
        sb.append(password).append(encryptUtils.getSalt(userId));
        return encryptUtils.encrypt(sb.toString());
    }
}
