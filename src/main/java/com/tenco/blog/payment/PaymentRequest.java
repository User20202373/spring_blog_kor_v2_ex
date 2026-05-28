package com.tenco.blog.payment;

import com.tenco.blog._core.errors.Exception400;
import lombok.Data;


public class PaymentRequest {
    @Data
    public static class PrepareDTO {
        private Integer amount; // 충전할 금액

        public void validate() {
            if (amount == null || amount <= 0) {
                throw new Exception400("충전할 포인트는 0보다 커야합니다");
            }
            if (amount < 1000) {
                throw new Exception400("최소 충전금액은 1000원 이상입니다");
            }
            if (amount > 100000) {
                throw new Exception400("최대 충전 금액은 100000원 미만입니다");
            }
        }
    }

    @Data
    public static class CompleteDTO{
        private String paymentId;

        public void validate(){
            if (this.paymentId == null || paymentId.trim().isEmpty()){
                throw new Exception400("결제 건 식별자가 필요합니다");
            }
        }

    }
}

