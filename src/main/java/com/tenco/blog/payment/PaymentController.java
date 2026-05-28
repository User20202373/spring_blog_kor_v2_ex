package com.tenco.blog.payment;

import com.tenco.blog._core.util.Define;
import com.tenco.blog.user.User;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    // /api/payment/prepare

    @PostMapping("/api/payment/prepare")
    public ResponseEntity<?> preparePayment(@RequestBody PaymentRequest.PrepareDTO reqDTO,
                                            HttpSession session) {

        // 1. 인증검사
        User sessionUser = (User) session.getAttribute(Define.SESSION_USER);
        if (sessionUser == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다"));
        }
        // 2. 유효성 검사
        reqDTO.validate();

        PaymentResponse.PrePareDTO prePareDTO = paymentService.결제요청생성(sessionUser.getId(), reqDTO.getAmount());
        return ResponseEntity.ok().body(
                Map.of("paymentId", prePareDTO.getPaymentId(),
                        "amount", prePareDTO.getAmount(),
                        "storeId", prePareDTO.getStoreId(),
                        "channelKey", prePareDTO.getChannelKey()));
    }

    // /api/payment/complete

    @PostMapping("/api/payment/complete")
    public ResponseEntity<?> completePayment(@RequestBody PaymentRequest.CompleteDTO reqDTO,
                                             HttpSession session) {

        // 인증검사
        User sessionUser = (User) session.getAttribute(Define.SESSION_USER);
        if (sessionUser == null) {
            ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다"));
        }
        // 유효성 검사
        reqDTO.validate();

        PaymentResponse.CompleteDTO completeDTO
                = paymentService.결제검증후포인트충전(sessionUser.getId(), reqDTO.getPaymentId());

        return ResponseEntity.ok().body(completeDTO);

    }
}
