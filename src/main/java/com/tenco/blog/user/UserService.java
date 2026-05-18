package com.tenco.blog.user;

import com.tenco.blog._core.errors.Exception400;
import com.tenco.blog._core.errors.Exception403;
import com.tenco.blog._core.errors.Exception404;
import com.tenco.blog._core.errors.Exception500;
import com.tenco.blog._core.util.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final UserRepository userRepository;

    //암호화 기능 DI
    private final PasswordEncoder passwordEncoder;

    /**
     * 회원 가입 처리
     *
     * @param joinDTO (사용자 회원가입 요청 정보)
     * @return User (저장된 사용자 정보)
     */
    @Transactional
    public User 회원가입(UserRequest.JoinDTO joinDTO) {
        log.info("회원가입 서비스 시작");
        // 회원 가입시 중복 체크
       userRepository.findByUsername(joinDTO.getUsername()).ifPresent(
               user ->{
                   throw new Exception400("이미 존재하는 사용자입니다");
               });

       // 프로필 이미지 저장 구현
        String profileImageFilename = null;
        //프로필 이미지가 비어있지 않을 때
        if (joinDTO.getProfileImage() != null && !joinDTO.getProfileImage().isEmpty()){
            try {
                // 이미지 파일이아닐 때
                if (!FileUtil.isImageFile(joinDTO.getProfileImage())){
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
        System.out.println("기존비번: " +joinDTO.getPassword());
        System.out.println("해쉬처리한 비번: "+hashPwd);

        // 해쉬 처리한 비번 user에 넣기
        user.setPassword(hashPwd);

        // 기본 권한 추가 (일반 사용자로 설정)
        user.addRole(Role.USER);

        return userRepository.save(user);
    }

    /**
     * 소셜 회원 가입 처리
     *
     * @param joinDTO (사용자 회원가입 요청 정보)
     * @return User (저장된 사용자 정보)
     */
    @Transactional
    public User 소셜회원가입(UserRequest.JoinDTO joinDTO, String profileImageUrl) {
        log.info("소셜회원가입 서비스 시작");

        userRepository.findByUsername(joinDTO.getUsername()).ifPresent(user -> {
            log.warn("소셜회원가입 실패 - 중복된 사용자명 : {}", user.getUsername());
            throw new Exception400("이미 존재하는 사용자 이름입니다");
        });

        // 프로필 이미지 저장 기능 구현(선택사항)
        String profileImageFilename = null;
        if (joinDTO.getProfileImage() != null && joinDTO.getProfileImage().isEmpty() == false) {
            // 이미지 파일 맞는지 검증

            try {
                if (FileUtil.isImageFile(joinDTO.getProfileImage()) == false) {
                    throw new Exception400("이미지 파일만 업로드 가능합니다");
                }
                profileImageFilename = FileUtil.saveFile(joinDTO.getProfileImage(), FileUtil.IMAGES_DIR);
            } catch (Exception e) {
                // 디스크 공간이 없거나 ,권한 없음
                throw new Exception500("프로필 이미지 저장 실패");
            }

        }

        // 코드 수정
        User user = joinDTO.toEntity(profileImageUrl);
        String hashPwd = passwordEncoder.encode(joinDTO.getPassword());
        System.out.println("rawPwd : " + joinDTO.getPassword());
        System.out.println("hashPwd : " + hashPwd);
        user.setPassword(hashPwd);


        // 기본 권한 추가 (일반 사용자로 설정)
        user.addRole(Role.USER);

        user.setOAuthProvider(OAuthProvider.KAKAO);

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
                .orElseThrow(() ->{
                    return new Exception400("사용자명 또는 비밀번호가 올바르지 않습니다");
                });

        // 2. 암호화 된 비밀번호 검증
        if (!passwordEncoder.matches(loginDTO.getPassword(), userEntity.getPassword())){
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
        }else {
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

        }else {
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
}




