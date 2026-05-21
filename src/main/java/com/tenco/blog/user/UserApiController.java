package com.tenco.blog.user;

// AJAX 통신 요청을 받아서 처리하는 컨트롤러

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController // Controller + Response
@RequiredArgsConstructor
public class UserApiController {

    private final MailService mailService;

    // 인증 번호 발송 API
    // 주소 설계 : http://localhost:8080/api/email/send , POST
    // @RequestBody <-- 클라이언트 단에서 application/json 형식으로 요청을 준다라고 약속
    // 생성된 HTTP 요청 메세지에서 요청 본문(body) 값을 추출해서 객체로 변환해
    @PostMapping("/api/email/send")
    public ResponseEntity<?> sendEmail(@RequestBody UserRequest.EmailCheckDTO reqDTO){
        reqDTO.validate();
        mailService.인증번호발송(reqDTO.getEmail());
        return ResponseEntity.ok("인증번호 발송 됨");
    }


}
