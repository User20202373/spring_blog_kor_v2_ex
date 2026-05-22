package com.tenco.blog.user;

import com.tenco.blog._core.errors.Exception400;
import com.tenco.blog._core.errors.Exception403;
import com.tenco.blog._core.errors.Exception404;
import com.tenco.blog._core.errors.Exception500;
import com.tenco.blog._core.util.FileUtil;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

/**
 * User 관련 비즈니스 로직을 처리하는 Service 계층
 * Controller 와 Repository 사이에서 실제 업무 로직을 담당
 */
@Slf4j
@Service // IoC
@RequiredArgsConstructor // DI
@Transactional(readOnly = true) // 기본적인 읽기 전용 트랜잭션 처리 , 조회시 더티 체킹 안 일어남
public class UserService {

    private final HttpSession session;
    private final UserRepository userRepository;

    //암호화 기능 DI
    private final PasswordEncoder passwordEncoder;


    // 초기 파라미터 값을 가져오는 방법
    @Value("${oauth.kakao.client-id}")
    private String kakaoClientId;

    @Value("${oauth.kakao.client-secret}")
    private String kakaoClientSecret;

    @Value("${tenco.key}")
    private String tencoKey;

    /**
     * 회원 가입 처리
     *
     * @param joinDTO (사용자 회원가입 요청 정보)
     * @return User (저장된 사용자 정보)
     */
    @Transactional
    public User 회원가입(UserRequest.JoinDTO joinDTO) {
        log.info("회원가입 서비스 시작");

        //[핵심] 이메일 인증 도장 확인
        String verifiedEmail = (String) session.getAttribute("verified_email");
        if(verifiedEmail == null || !verifiedEmail.equals(joinDTO.getEmail())) {
            // 이메일 위변조를 방지하기 위해 인증번호 검증시 넣었던 그 이메일 진행 시켜야 한다
            throw new Exception400("이메인 인증을 완료해주세요");
        }

        // 회원 가입시 아이디 중복 체크
        userRepository.findByUsername(joinDTO.getUsername()).ifPresent(
                user -> {
                    log.warn("회원가입 실패 - 중복된 사용자명 : {}", user.getUsername());
                    throw new Exception400("이미 존재하는 사용자입니다");
                });

         userRepository.findByEmail(joinDTO.getEmail()).ifPresent(
                 user -> {
                     log.warn("회원가입 실패 - 중복된 이메일 : {}", user.getEmail());
                     throw new Exception400("이미 존재하는 이메일입니다");
                 });


        // 프로필 이미지 저장 구현
        String profileImageFilename = null;
        //프로필 이미지가 비어있지 않을 때
        if (joinDTO.getProfileImage() != null && !joinDTO.getProfileImage().isEmpty()) {
            try {
                // 이미지 파일이아닐 때
                if (!FileUtil.isImageFile(joinDTO.getProfileImage())) {
                    throw new Exception400("이미지파일만 업로드 가능합니다");
                }
                //이미지 파일일 때 저장하는 기능
                profileImageFilename = FileUtil.saveFile(joinDTO.getProfileImage(), FileUtil.IMAGES_DIR);
            } catch (IOException e) {
                // 디스크 공간 없거나, 권한 없음
                throw new Exception500("프로필 이미지 저장 실패");
            }
        }


        //비밀번호 암호화 기능 추가
        User user = joinDTO.toEntity(profileImageFilename);
        String hashPwd = passwordEncoder.encode(joinDTO.getPassword());
        // 해쉬 처리한 비번 user에 넣기
        user.setPassword(hashPwd);

        //[핵심] 이메일 인증 도장 삭제
        session.removeAttribute("verified_email");
        return userRepository.save(user);
    }


    /**
     * 로그인 처리
     *
     * @param loginDTO (사용자가 요청한 로그인 정보)
     * @return User(조회된 정보 세션 저장용)
     */
    public User 로그인(UserRequest.LoginDTO loginDTO) {
        // 1. 사용자 계정 여부 확인
        User userEntity = userRepository.findByUsernameAndWithRoles(loginDTO.getUsername())
                .orElseThrow(() -> {
                    return new Exception400("사용자명 또는 비밀번호가 올바르지 않습니다");
                });

        // 2. 암호화 된 비밀번호 검증
        if (!passwordEncoder.matches(loginDTO.getPassword(), userEntity.getPassword())) {
            throw new Exception400(" 사용자명 또는 비밀번호가 올바르지 않습니다");
        }

        return userEntity;
    }

    /**
     * 사용자 정보 조회 (프로필 정보 보기 활용)
     *
     * @param id (User PK)
     * @return UserEntity
     */
    public User 회원정보수정화면(Integer id) {
        log.info("사용자 정보 서비스 시작");
        User userEntity = userRepository.findById(id).orElseThrow(() -> {
            log.warn("사용자 정보 조회 실패");
            return new Exception404("사용자 정보를 찾을 수 없습니다");
        });
        return userEntity;
    }


    /**
     * 사용자 정보 수정 처리 (프로필 업데이트)
     *
     * @param id        (User PK)
     * @param updateDTO (사용자가 요청한 데이터)
     * @return User
     */
    @Transactional
    public User 회원정보수정(Integer id, UserRequest.UpdateDTO updateDTO) {
        log.info("회원정보 서비스 시작");

        String newPassword = null;
        String newProfileImageFilename = null;
        // 1. 항상 조회부터
        User userEntity = userRepository.findById(id).orElseThrow(
                () -> new Exception404("사용자 정보를 찾을 수 없습니다"));

        // 2. 권한 확인
        if (!userEntity.getId().equals(id)) {
            throw new Exception403("회원정보 수정 권한이 없습니다");

        }
        // 3. 로직처리 1 - 사용자가 비밀번호를 입력 했을 경우 갱신
        if (updateDTO.getPassword() != null && !updateDTO.getPassword().isBlank()) {
            // 여기서 유효성 검사 해야 됨.
            updateDTO.validate();
            String rawPassword = updateDTO.getPassword();
            updateDTO.setPassword(passwordEncoder.encode(rawPassword));
        } else {
            updateDTO.setPassword(null);
        }

        // 4. 로직 처리 2 - 사용자가 새로운 이미지를 등록했을 경우
        if (updateDTO.getProfileImage() != null && !updateDTO.getProfileImage().isEmpty()) {
            try {
                if (!FileUtil.isImageFile(updateDTO.getProfileImage())) {
                    throw new Exception400("이미지 파일만 업로드 가능합니다");
                }
                // 새 이미지 로컬 폴더에 저장( 중복되지 않을 이미지 파일 이름을 리턴)
                newProfileImageFilename = FileUtil.saveFile(updateDTO.getProfileImage(), FileUtil.IMAGES_DIR);
                updateDTO.setProfileImageFileName(newProfileImageFilename);

                // 기존 이미지 파일 삭제 해야 함( 로컬에 계속 파일 쌓임)
                String oldProfileImageFileName = userEntity.getProfileImage();
                if (oldProfileImageFileName != null) {
                    FileUtil.deleteFile(oldProfileImageFileName, FileUtil.IMAGES_DIR);
                }

            } catch (IOException e) {
                throw new Exception400("파일 저장에 실패");
            }

        } else {
            updateDTO.setProfileImageFileName(userEntity.getProfileImage());
        }
        //더티체킹
        userEntity.update(updateDTO);
        return userEntity;
    }

    @Transactional
    public User 프로필이미지삭제(Integer id) {
        // 1. 정보 조회
        User userEntity = userRepository.findById(id).orElseThrow(
                () -> new Exception404("사용자를 찾을 수 없습니다"));
        // 2. 인가 처리
        if (userEntity.getId().equals(id) == false) {
            throw new Exception403("프로필 이미지 삭제 권한 없음");
        }

        // 3. 이미지가 등록되어 있으면 삭제 처리
        String profileImage = userEntity.getProfileImage();
        if (profileImage != null && !profileImage.isEmpty()) {
            try {
                FileUtil.deleteFile(profileImage, FileUtil.IMAGES_DIR);
            } catch (IOException e) {
                System.err.println("프로필 이미지 삭제시 오류 발생" + e.getMessage());
            }
        }
        // 1차 캐쉬에 저장된 User 정보 수정 - 트랜잭션이 종료되면 반영(더티 체킹)
        userEntity.setProfileImage(null);
        return userEntity;

    }

    public User 사용자이름조회(String username) {
        return userRepository.findByUsername(username).orElse(null);

    }

    // 1.
    private UserResponse.OAuthToken 카카오액세스토큰발급(String code) {
        RestTemplate restTemplate1 = new RestTemplate();

        // 헤더
        HttpHeaders headers1 = new HttpHeaders();
        headers1.add("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

        // 바디
        LinkedMultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap<>();
        multiValueMap.add("grant_type", "authorization_code");
        multiValueMap.add("client_id", kakaoClientId);
        multiValueMap.add("redirect_uri", "http://localhost:8080/kakao-redirect");
        multiValueMap.add("code", code);
        // 최신 사항: 반드시 시크릿키 body에 설정
        multiValueMap.add("client_secret", kakaoClientSecret);

        // 순서 중요 바디+헤더 결합(HTTP 요청 메세지 구축)
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(multiValueMap, headers1);

        // HTTP 요청 후 응답
        ResponseEntity<UserResponse.OAuthToken> response1 = restTemplate1.exchange("https://kauth.kakao.com/oauth/token",
                HttpMethod.POST,
                request,
                UserResponse.OAuthToken.class
        );
        UserResponse.OAuthToken oAuthToken = response1.getBody();
        return oAuthToken;
    }

    // 2단계.
    //  UserResponse.KakaoProfile.KakaoAccount.Profile profile
    private UserResponse.KakaoProfile 카카오프로필조회(String token) {
        // 발급 받은 액세스 토큰으로 해당 사용자의 정보 요청
        String accessToken = token;
        RestTemplate restTemplate2 = new RestTemplate();

        HttpHeaders headers2 = new HttpHeaders();
        //주의! 반드시 Bearer + "공백한칸" + 토큰
        headers2.add("Authorization", "Bearer " + accessToken);
        headers2.add("Content-Type",
                "application/x-www-form-urlencoded;charset=utf-8");

        HttpEntity request2 = new HttpEntity(headers2);

        // HTTP  요청 2
        ResponseEntity<UserResponse.KakaoProfile> response2 = restTemplate2.exchange(
                "https://kapi.kakao.com/v2/user/me",
                HttpMethod.POST,
                request2,
                UserResponse.KakaoProfile.class
        );
        UserResponse.KakaoProfile kakaoProfile = response2.getBody();
        return kakaoProfile;

    }

    // 3단계.
    private User 카카오조회및자동회원가입처리(UserResponse.KakaoProfile kakaoProfile) {
        // 고유한 username 생성(중복 방지용)
        String username = kakaoProfile.getKakaoAccount().getProfile().getNickname() + "_" + kakaoProfile.getId();
        // 회원 가입 여부 확인
        User user = 사용자이름조회(username);
        if (user == null) {
            log.info("기존 회원이 아님 자동 회원 가입 진행");
            User newUser = User.builder()
                    .username(username)
                    .password(passwordEncoder.encode(tencoKey))// 임시 비밀번호(노출 절대 금지)
                    .email(username + "@kakao.com") // 임의 이메일 설정 ( 추후 DB 제약 방지)
                    .oAuthProvider(OAuthProvider.KAKAO) // 로그인 경로 설정
                    .build();
            String profileImage = kakaoProfile.getKakaoAccount().getProfile().getProfileImageUrl();
            if (profileImage != null && !profileImage.isEmpty()) {
                newUser.setProfileImage(profileImage); // 카카오 서버에서 받은 이미지 설정
            }

            // 회원 가입 후 상태 저장
            user = userRepository.save(newUser);
        } else {
            System.out.println("이미 가입된 사용자라 바로 로그인 처리");
        }
        return user;
    }


    // 0단계
    @Transactional
    public User 카카오소셜로그인(String code) {

        // 1. 발급 받은 인가 코드로 액세스 토큰 발급 요청
        UserResponse.OAuthToken oAuthToken = 카카오액세스토큰발급(code);

        // 2. 발급 받은 액세스 토큰으로 사용자 카카오 프로필 조회
        UserResponse.KakaoProfile kakaoProfile = 카카오프로필조회(oAuthToken.getAccessToken());


        // 3. 응답 받은 결과로 우리 서버에 가입여부 조회 및 자동 회원가입 처리
        User userEntity = 카카오조회및자동회원가입처리(kakaoProfile);


        // 4.  컨트롤러로 User 반환
        return userEntity;
    }


}




