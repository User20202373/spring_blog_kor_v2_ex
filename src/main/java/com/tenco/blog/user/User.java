package com.tenco.blog.user;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@Table(name = "user_tb")
@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    // 사용자명 중복 방지를 위한 유니크 제약 조건 설정
    @Column(unique = true)
    private String username;

    //User 테이블에는 이미지 파일명만 저장할 예정 ( 실제 데이터는 내 서버 컴퓨터 로컬에 저장할 예정)
    @Column(nullable = true) // null값 허용, 기본값
    private String profileImage; // 프로필 이미지는 선택 사항(회원가입시)

    private String password;
    private String email;
    // 엔티티가 영속화 될 때 자동으로 현재 시간을 주입해라 pc -> db
    @CreationTimestamp
    private Timestamp createdAt;

    @Builder
    public User(Integer id, String username, String password, String email, Timestamp createdAt,String profileImage) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.createdAt = createdAt;
        this.profileImage = profileImage;
    }


    // 편의 기능 추가 - 회원 정보 수정
    public void update(UserRequest.UpdateDTO updateDTO,String newProfileImageFileName) {
        this.password = updateDTO.getPassword();
        this.profileImage = newProfileImageFileName;
        // Dirty Checking 처리
    }
}
