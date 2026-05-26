package com.tenco.blog.board;

import com.tenco.blog._core.errors.Exception403;
import com.tenco.blog._core.util.MyDateUtil;
import com.tenco.blog.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

@Data // get,set, toString ..
// @Entity - JPA가 이 클래스를 데이터베이스 테이블과 매핑하는 객체로 인식하게 설정
// 즉, 이 어노테이션이 있어야 JPA가 관리 함
@Entity
@Table(name = "board_tb")
@NoArgsConstructor
@AllArgsConstructor // 전체 멤벼 번수를 넣을 수 있는 생성자.
@Builder
public class Board {

    // @id : 이 필드가 기본키임을 설정 함
    @Id
    // IDENTITY 전략: 데이터베이스게 기본 AUTO_INCREMENT 기능 사용
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String title;
    private String content;


    @ColumnDefault("false")
    @Builder.Default // 이게 없으면 다른 파일에서 Build 객체 생성시 null 값이 셋팅됨
    private Boolean premium = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // 외래키 컬럼명 표시 됨
    private User user;


    @CreationTimestamp
    private Timestamp createdAt;

    // createdAt -> 포멧 하는 메서드 만들어 보기
    public String getTime() {
        return MyDateUtil.timestampFormat(createdAt);
    }

    // 수정 편의 기능 만들기
    public void update(BoardRequest.UpdateDTO updateDTO) {
        // this.username = updateDTO.getUsername(); 삭제 예정
        this.title = updateDTO.getTitle();
        this.content = updateDTO.getContent();
        //유료 여부도 함께 수정
        this.premium = (updateDTO.getPremium() !=null ? updateDTO.getPremium() : false);

    }

    // 편의 기능 - 게시글 소유자 확인을 위한 기능 추가
    public boolean isOwner(Integer sessionUserId) {
       if(!this.user.getId().equals(sessionUserId)) {
           throw new Exception403("본인이 작성한 게시글이 아닙니다");
       }
       return true;
    }

}

