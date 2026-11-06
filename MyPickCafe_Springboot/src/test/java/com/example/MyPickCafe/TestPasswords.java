package com.example.MyPickCafe;

/**
 * 테스트에서 회원을 직접 저장할 때 쓰는 공용 비밀번호.
 * 원문은 비밀번호 정책(8~20자, 공백 없음, 흔한 비밀번호·이메일 아이디·닉네임 미포함)을 지키고,
 * HASH 는 그 원문의 BCrypt(cost 10) 해시다. 로그인까지 해 보는 테스트는 RAW 로 로그인하면 된다.
 */
public final class TestPasswords {

    public static final String RAW = "quiet-latte-8642";
    public static final String HASH = "$2a$10$RddE4f4ICAt9JIFRGgIMY.DSloFanr4MtpX2Lr9AZRFCi68PUQDD.";

    private TestPasswords() {}
}
