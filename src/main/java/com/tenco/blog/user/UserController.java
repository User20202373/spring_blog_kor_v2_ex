package com.tenco.blog.user;

import com.tenco.blog._core.util.Define;
import com.tenco.blog._core.util.FileUtil;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Slf4j
@Controller // IoC
@RequiredArgsConstructor // DI 처리
public class UserController {

    private final UserService userService;

    // 프로필 이미지 삭제 요청
    @PostMapping("/user/profile-image/delete")
    public String deleteProfileImage(HttpSession session) {
        // 이미지 삭제를 하기 위해 들고옴
        User sessionUser = (User) session.getAttribute(Define.SESSION_USER);
        // 프로필 이미지 삭제
        User updateUser = userService.프로필이미지삭제(sessionUser.getId());
        // 삭제 할 때 세션에 저장되어 있던 프로필 이미지 삭제 후 세션 동기화 처리
        session.setAttribute(Define.SESSION_USER, updateUser);
        return "redirect:/user/detail";
    }

    //마이페이지 요청 화면
    @GetMapping("/user/detail")
    public String detailPage(Model model, HttpSession session) {

        User sessionUser = (User) session.getAttribute(Define.SESSION_USER);
        model.addAttribute("user", sessionUser);
        return "user/detail";
    }

    // 회원 정보 수정 기능요청
    @PostMapping("/user/update")
    public String updateProc(UserRequest.UpdateDTO updateDTO, HttpSession session) {
        // 회원 정보 수정 요청시 기본 비밀번호 null이고 프로필 이미지만 수정 요청
        User sessionUser = (User) session.getAttribute(Define.SESSION_USER);
        // 프로필 이미지 변경 요청이 왔을 때 기존에 비밀번호 저장
        if (updateDTO.getPassword() == null || updateDTO.getPassword().isBlank()){
            updateDTO.setPassword(sessionUser.getPassword());
        }
        //인증검사
        updateDTO.validate();
        User updateUser = userService.회원정보수정(sessionUser.getId(), updateDTO);
        session.setAttribute("sessionUser", updateUser);
        return "redirect:/";
    }

    // 프로필 화면 요청
    @GetMapping("/user/update-form")
    public String updateFormPage(HttpSession session, Model model) {
        User sessionUser = (User) session.getAttribute("sessionUser");
        User user = userService.회원정보수정화면(sessionUser.getId());
        model.addAttribute("user", user);
        return "user/update-form";
    }

    // 로그인 화면 요청
    // 주소 설계 - http://localhost:8080/login-form
    @GetMapping("/login-form")
    public String loginFormPage() {
        return "user/login-form";
    }

    // 로그인 기능 요청
    @PostMapping("/login")
    public String loginProc(UserRequest.LoginDTO reqLoginDTO, HttpSession session) {
        // 인증 검사 x, 유효성 검사 o
        reqLoginDTO.validate();
        User user = userService.로그인(reqLoginDTO);
        session.setAttribute("sessionUser", user);
        return "redirect:/";
    }


    // 로그아웃 기능 요청
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        // 세션 메모리에 내 정보를 없애 버림
        session.invalidate();
        return "redirect:/";
    }

    // 회원 가입 화면 요청
    // 주소 설계 - http://localhost:8080/join-form
    @GetMapping("/join-form")
    public String joinFormPage() {
        return "user/join-form";
    }

    // 회원 가입 기능 요청
    // 주소 설계 - http://localhost:8080/join
    @PostMapping("/join")
    public String joinProc(UserRequest.JoinDTO joinDTO) {
        //  인증검사 x, 유효성 검사 하기 o
        joinDTO.validate();
        userService.회원가입(joinDTO);
        return "redirect:/login-form";
    }

}
