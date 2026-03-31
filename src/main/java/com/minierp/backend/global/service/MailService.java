package com.minierp.backend.global.service;

import com.minierp.backend.global.exception.BusinessException;
import com.minierp.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:}")
    private String fromAddress;

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    public void sendVerificationCodeMail(String toEmail, String verificationCode) {
        if (!mailEnabled) {
            log.info("메일 발송 비활성화 상태입니다. code={}, to={}", verificationCode, toEmail);
            return;
        }

        String resolvedFrom = resolveFromAddress();
        if (resolvedFrom == null || resolvedFrom.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_RESET_REQUEST, "메일 발신 주소 설정이 필요합니다.");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setFrom(resolvedFrom);
        message.setSubject("[Mini ERP] 비밀번호 재설정 인증번호");
        message.setText(buildVerificationCodeBody(verificationCode));

        try {
            mailSender.send(message);
        } catch (MailException e) {
            throw new BusinessException(ErrorCode.INVALID_RESET_REQUEST, "인증 메일 발송에 실패했습니다.");
        }
    }

    private String resolveFromAddress() {
        if (fromAddress != null && !fromAddress.isBlank()) {
            return fromAddress;
        }

        String envFrom = System.getenv("GMAIL_FROM");
        if (envFrom != null && !envFrom.isBlank()) {
            return envFrom;
        }

        if (smtpUsername != null && !smtpUsername.isBlank()) {
            return smtpUsername;
        }

        String envUsername = System.getenv("GMAIL_USERNAME");
        if (envUsername != null && !envUsername.isBlank()) {
            return envUsername;
        }

        return null;
    }

    private String buildVerificationCodeBody(String verificationCode) {
        return "안녕하세요. Mini ERP 비밀번호 재설정 인증번호입니다.\n\n"
                + "인증번호: " + verificationCode + "\n"
                + "유효시간: 5분\n\n"
                + "본인이 요청하지 않았다면 이 메일을 무시해주세요.";
    }
}
