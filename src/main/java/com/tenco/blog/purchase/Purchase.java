package com.tenco.blog.purchase;

import com.tenco.blog.board.Board;
import com.tenco.blog.user.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

/**
 * 구매 내역 엔티티
 *
 * User 와 Board 의 구매 이력 관계를 표현 함
 *
 * 한 사람에 사용자는 여러 게시글을 구매할 수 있다. (o)
 * 한 게시글을 여러 사용자에게 구매 될 수 있다. (o)
 * User : Board - 다대다 관계로 표현이 되기 때문게 중간 테이블 (Purchase) 생성이 되어야 한다
 * Purchase : User --> @ManyToOne --> join column 이름 지정
 * Purchase : Board --> @ManyToOne --> join column 이름 지정
 * 복합키 설정 방법도 확인 (테이블 기준으로 어노테이션 설정)
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "purchase_tb",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_board", columnNames = {"user_id", "board_id"})
        })
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 단방향 관계 " Purchase -> User "
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // 단방향 관계 " Purchase -> Board "
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private Board board;

    // 구매시 지불한 포인트
    private Integer price;

    @CreationTimestamp // pc -> db 자동 주입
    private Timestamp createdAt;

    @Builder
    public Purchase(User user, Board board, Integer price) {
        this.user = user;
        this.board = board;
        this.price = price;
    }
}