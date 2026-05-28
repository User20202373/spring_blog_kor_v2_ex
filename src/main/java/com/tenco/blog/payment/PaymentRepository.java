package com.tenco.blog.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    // 주문번호(paymentId) 중복 확인 조회
    @Query("SELECT COUNT(p) > 0 FROM Payment p WHERE p.paymentId = :paymentId")
    boolean existsByPaymentId(@Param("paymentId") String paymentId);
}
