package com.tenco.blog.user;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "user_role_tb",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_role", columnNames = {"user_id,", "role"})
        })
public class UserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // private Integer user_id 컬럼은 User 부모 엔티티에서 명시가 되어 자동 생성 됨.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role; //enum 타입

    @Builder
    public UserRole( Integer id, Role role) {
        // id는 Userrole 에 PK이다
        this.id = id;
        this.role = role;
    }
}
