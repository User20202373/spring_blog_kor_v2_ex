package com.tenco.blog.user;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

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


    // User : UserRole 연관 관계를 단방향 1: N 구조 설계
    // DB 기준으로 FK 컬럼이나(키는) 1 : N 구조에서 항상 N이 FK키를 가지고 있다
    // JPA 1 : N 구조일 경우 (User , UserRole) , @JoinColumn(name="user_id") 의미는
    // 여기 테이블에 컬럼 user_id 생성해 라는 의미이다. 그런데 1 : N 구조에서 FK 컬럼이
    // 1쪽 테이블에 생성되는 경우는 없다. 무조건 N쪽에 FK 컬럼이 만들어져야하기 때문에
    // 자동으로 User 테이블에  @JoinColumn("user_id") 하더라도 알아서 UserRole 컬럼을 자기가 생성한다

    /**
     * 사용자 권한 목록
     * User (1) : UserRole (N) 연관 관계를 정의 함
     * <p>
     * 1. @OneToMany + @Joincolumn(name = "user_id")
     * - User 가 UserRole 리스트를 관리한다. (단방향)
     * - 실제 DB user_role_tb 테이블에 FK 컬럼은 user_id 명이 user_role_tb에 생성된다
     * <p>
     * 2. cascadeType.ALL (운명공동체)
     * Java 기준에서 User 저장하면 Role 도 자동 저장되고 , User삭제하면 가지고 있던
     * Role들도 다 삭제가 됩니다. DB 에서 실제 delete 쿼리가 발생됩니다
     * <p>
     * 3. orphanRemoval (리스트와 DB를 동기화)
     * DB에서 실제 delete 쿼리가 발생된다. = true 처리
     * <p>
     * 4. fetch = FetchType.EAGER (특별취급)
     * 데이터 양이 얼마 되지 않음. 그래서 한 번에 데이터를 채워서 가지고 오는 것이 편리함
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private List<UserRole> roles = new ArrayList<>();

//    @Enumerated(EnumType.STRING) // db랑 java랑 다르기 때문에 db에서 STRING 타입으로 관리하라고 선언
//    @Column(nullable = false) //null 허용 안함
//    @ColumnDefault("'LOCAL'") // 어노테이션으로 디폴트값 선언 방법 (문자열일 경우 ' ' 반드시 사용)
//    private OAuthProvider oAuthProvider;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'LOCAL'")
    private OAuthProvider oAuthProvider;

    @Builder
    public User(Integer id, String username, String password, String email, Timestamp createdAt,
                String profileImage, OAuthProvider oAuthProvider, List<UserRole> roles) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.createdAt = createdAt;
        this.profileImage = profileImage;

        this.oAuthProvider = (oAuthProvider != null) ? oAuthProvider : OAuthProvider.LOCAL;

        // 1. roles(ArrayList 타입)가 null이면 빈 리스트로 초기화(NPE 방지 처리)
        this.roles = (roles != null) ? roles : new ArrayList<>();

        // 2. roles 가 비어있으면 - USER 기본 권한으로 자동 설정
        if (this.roles.isEmpty()) {
            // new UserRole(Role.USER); 랑 빌더패턴이랑 같음
            this.roles.add(UserRole.builder().role(Role.USER).build());
        }

    }

    // 편의 기능 추가 - 회원 정보 수정
    public void update(UserRequest.UpdateDTO updateDTO) {
        if (updateDTO.getPassword() != null) {
            // 암호화 변경되어 들어 옴
            this.password = updateDTO.getPassword();
        }

        if (updateDTO.getProfileImageFileName() != null) {
            this.profileImage = updateDTO.getProfileImageFileName();
        }


    }

    // User 엔티티에 권한 관련 편의 기능 만들어 보기

    // Role 추가 편의 메서드
    //Role.ADMIN , Role.USER
    public void addRole(Role role) {
        //this.roles.get(0) = new UserRole(1,Role.USER);
        this.roles.add(UserRole.builder()
                .role(role)
                .build());
    }

    // 해당 Role을 가지고 있는 여부 확인
    // boolean isAdmin = user.hasRole(Role.ADMIN);
    public boolean hasRole(Role role) {
        // 1. 방어적 코드 작성
        if (this.roles == null || this.roles.isEmpty()) {
            // Role (해당 유저에 대한 권한이) 자체가 설정 되지 않은 상태
            return false;
        }

        for (UserRole userRole : this.roles) {
            if (userRole.getRole() == role) {
                return true;
            }
        }
        return false;
    }

    // 관리자 여부 확인 메서드 - 머스태치에서 is 생략하고 admin으로 접근 가능함
    public boolean isAdmin() {
        return hasRole(Role.ADMIN);
    }

    // 머스태치 화면에서 사용할 편의 메서드 1
    public String getRoleDisplay() {
        //isAdmin()이 true 라면 "ADMIN" 반환 false라면 "USER"반환
        return isAdmin() ? "ADMIN" : "USER";
    }

    // 머스태치 화면에서 사용할 편의 메서드 2
    //OAuthProvider 값에 따라서 경로 변수를 다르게 리턴
    public String getProfilePath() {
        if (this.profileImage == null) {
            return null;
        }
        // 이미지 경로가 http로 시작 (소셜가입)
        if (this.profileImage.startsWith("http")) {
            return this.profileImage;
        }
        // 로컬 이미지(서버 기준 경로)
        return "/images/" + this.profileImage;
    }

    // 머스태치 화면에서 사용할 편의 메서드 3
    public boolean isLocal() {
        // true -> 이메일 가입자를 의미함
        return this.oAuthProvider == OAuthProvider.LOCAL;
    }
}
