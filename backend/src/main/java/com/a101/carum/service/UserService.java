package com.a101.carum.service;

import com.a101.carum.api.dto.*;
import com.a101.carum.common.exception.RefreshFailException;
import com.a101.carum.common.exception.UnAuthorizedException;
import com.a101.carum.domain.history.History;
import com.a101.carum.domain.question.FaceType;
import com.a101.carum.domain.room.Room;
import com.a101.carum.domain.user.User;
import com.a101.carum.domain.user.UserDetail;
import com.a101.carum.repository.HistoryRepository;
import com.a101.carum.repository.RoomRepository;
import com.a101.carum.repository.UserDetailRepository;
import com.a101.carum.repository.UserRepository;
import com.a101.carum.util.EncryptUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserDetailRepository userDetailRepository;
    private final RoomRepository roomRepository;
    private final HistoryRepository historyRepository;
    private final JwtService jwtService;
    private final TemplateConversionService templateConversionService;
    private final EncryptUtils encryptUtils;
    private final RedisTemplate<String, String> tokenRedisTemplate;
    private final RedisTemplate<String, Long> idRedisTemplate;

    private int REFRESH_MINUTES;
    private final int KEY_STRETCH = 4;
    private final String[] emotions = {"HAPPY", "ANGRY", "SAD", "SURPRISE", "WORRY", "PEACE"};

    @Value("${jwt.token.time.refresh}")
    public void setREFRESH_MINUTES(String refreshMinutes){
        this.REFRESH_MINUTES = Integer.parseInt(refreshMinutes);
    }
    @Transactional
    public void createUser(ReqPostUser reqPostUser) throws NoSuchAlgorithmException {

        if(userRepository.existsByNickNameAndIsDeleted(reqPostUser.getNickName(), false) ||
        userRepository.existsByUserIdAndIsDeleted(reqPostUser.getUserId(), false)){
            throw new UnAuthorizedException("정보가 중복됩니다.");
        }

        String password = encryptPassword(reqPostUser.getUserId(), reqPostUser.getPassword());

        User user = User.builder()
                .userId(reqPostUser.getUserId())
                .birth(reqPostUser.getBirth())
                .nickName(reqPostUser.getNickName())
                .password(password)
                .phone(reqPostUser.getPhone())
                .build();

        user = userRepository.save(user);

        templateConversionService.createNewRoomAll(user);

        Room room = roomRepository.findTop1ByUserOrderByIdAsc(user)
                .orElseThrow(() -> new NullPointerException("Room을 찾을 수 없습니다."));;

        UserDetail userDetail = UserDetail.builder()
                .user(user)
                .mainRoom(room)
                .build();
        userDetail.updateMoney(500L, '+');

        userDetailRepository.save(userDetail);

        for(String e: emotions){
            historyRepository.save(History.builder()
                    .user(user)
                    .year(LocalDate.now().getYear())
                    .month(LocalDate.now().getMonthValue())
                    .emotion(e)
                    .count(0L)
                    .build());
        }
    }

    public ResLoginUser loginUser(ReqLoginUser reqLoginUser) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        String password = encryptPassword(reqLoginUser.getUserId(), reqLoginUser.getPassword());

        User user = userRepository.findByUserIdAndPasswordAndIsDeleted(reqLoginUser.getUserId(), password, false)
                .orElseThrow(() -> new NullPointerException("User를 찾을 수 없습니다."));

        String accessToken = jwtService.createToken(user);
        String refreshToken = jwtService.createRefreshToken();

        setTokenInRedis(accessToken, refreshToken, user.getId());

        return ResLoginUser.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public void logoutUser(String accessToken) {
        tokenRedisTemplate.delete(accessToken);
    }

    @Transactional
    public ResGetUser readUser(Long id) {
        ResGetUser.ResGetUserBuilder resGetUserBuilder = ResGetUser.builder();

        // User Table에서 정보 가져오기
        User user = userRepository.findByIdAndIsDeleted(id, false)
                .orElseThrow(() -> new NullPointerException("User를 찾을 수 없습니다."));
        resGetUserBuilder
                .userId(user.getUserId())
                .nickName(user.getNickName())
                .birth(user.getBirth())
                .phone(user.getPhone());

        // User Detail에서 정보 가져오기
        UserDetail userDetail = userDetailRepository.findByUser(user)
                .orElseThrow(() -> new NullPointerException("User 정보가 손상되었습니다."));
        resGetUserBuilder
                .money(userDetail.getMoney())
                .petType(userDetail.getPetType());

        Room room = userDetail.getMainRoom();

        ResGetRoom resGetRoom = ResGetRoom.builder()
                .id(room.getId())
                .name(room.getName())
                .background(room.getBackground())
                .frame(room.getFrame())
                .emotionTag(
                        room.getEmotionTag() == null ?
                             new ArrayList<>()
                            :List.of(room.getEmotionTag().split(","))
                )
                .build();

        resGetUserBuilder
                .mainRoom(resGetRoom);

        if(userDetail.getLastDiary() != null && userDetail.getLastDiary().equals(LocalDate.now())) {
            resGetUserBuilder
                    .dailyColor(userDetail.getDailyColor())
                    .dailyFace(userDetail.getDailyFace())
                    .todayDiary(true);
        } else {
            resGetUserBuilder
                    .dailyFace(FaceType.NORMAL)
                    .dailyColor(0)
                    .todayDiary(false);
        }

        return resGetUserBuilder.build();
    }

    public void readUserId(ReqGetUserId reqGetUserId) throws SQLIntegrityConstraintViolationException {
        if (userRepository.existsByUserIdAndIsDeleted(reqGetUserId.getUserId(), false)) {
            throw new SQLIntegrityConstraintViolationException("아이디 중복입니다.");
        }
    }

    public void readNickName(ReqGetNickName reqGetNickName) throws SQLIntegrityConstraintViolationException {
        if (userRepository.existsByNickNameAndIsDeleted(reqGetNickName.getNickName(), false)) {
            throw new SQLIntegrityConstraintViolationException("닉네임 중복입니다.");
        }
    }

    @Transactional
    public void updateUser(ReqPatchUser reqPatchUser, Long id) {
        User user = userRepository.findByIdAndIsDeleted(id, false)
                .orElseThrow(() -> new NullPointerException("User를 찾을 수 없습니다."));
        user.updateNickName(reqPatchUser.getNickName());
    }

    @Transactional
    public void updateUserPassword(ReqPatchUserPassword reqPatchUserPassword, Long id) throws NoSuchAlgorithmException {
        User user = userRepository.findByIdAndIsDeleted(id, false)
                .orElseThrow(() -> new NullPointerException("User를 찾을 수 없습니다."));

        String oldPassword = encryptPassword(user.getUserId(), reqPatchUserPassword.getOldPassword());

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

    public ResLoginUser updateAccessToken(String accessToken, String refreshToken) throws UnsupportedEncodingException {

        String redisRefreshToken = tokenRedisTemplate.opsForValue().get(accessToken);

        if(redisRefreshToken == null || !jwtService.checkJwtToken(refreshToken) || !redisRefreshToken.equals(redisRefreshToken)){
            throw new RefreshFailException("다시 로그인 하십시오");
        }

        Long id = idRedisTemplate.opsForValue().get(accessToken);
        User user = userRepository.findByIdAndIsDeleted(id, false)
                .orElseThrow(() -> new NullPointerException("User를 찾을 수 없습니다."));

        String newAccessToken = jwtService.createToken(user);
        String newRefreshToken = jwtService.createRefreshToken();

        tokenRedisTemplate.rename(accessToken, newAccessToken);
        idRedisTemplate.rename(accessToken, newAccessToken);
        setTokenInRedis(newAccessToken, newRefreshToken, user.getId());

        return ResLoginUser.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    public String encryptPassword(String userId, String password) throws NoSuchAlgorithmException {
        StringBuilder sb = new StringBuilder();
        sb.append(password).append(encryptUtils.getSalt(userId));
        String ret = sb.toString();
        for(int i = 0; i < KEY_STRETCH; i++){
            ret = encryptUtils.encrypt(ret);
        }
        return ret;
    }

    public void setTokenInRedis(String accessToken, String refreshToken, Long id) {
        tokenRedisTemplate.opsForValue().set(accessToken, refreshToken);
        tokenRedisTemplate.expire(accessToken, REFRESH_MINUTES * 60 ,TimeUnit.SECONDS);
        idRedisTemplate.opsForValue().set(accessToken, id);
    }
}
