package com.coffiness.calfit.support.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

  private final JavaMailSender mailSender;
  private final TemplateEngine templateEngine;

  // 내부용 공통 텍스트 발송 (외부 직접 호출 X)
  private void sendEmail(String from, String to, String subject, String content) {
    try {
      SimpleMailMessage message = new SimpleMailMessage();
      message.setFrom(from);
      message.setTo(to);
      message.setSubject(subject);
      message.setText(content);
      mailSender.send(message);
      log.info("이메일 전송 성공: [{}] -> [{}]", from, to);
    } catch (MailException e) {
      log.error("이메일 전송 실패: [{}] -> [{}]", from, to, e);
      throw new RuntimeException("이메일 전송 중 오류가 발생했습니다.", e);
    }
  }

  // 내부용 공통 HTML 발송 (외부 직접 호출 X)
  private void sendHtmlEmail(String from, String to, String subject, String htmlContent) {
    try {
      MimeMessage mimeMessage = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
      helper.setFrom(from);
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(htmlContent, true);
      mailSender.send(mimeMessage);
      log.info("HTML 이메일 전송 성공: [{}] -> [{}]", from, to);
    } catch (MessagingException e) { // Exception -> MessagingException으로 구체화
      log.error("HTML 이메일 전송 실패: [{}] -> [{}]", from, to, e);
      throw new RuntimeException("HTML 이메일 전송 중 오류가 발생했습니다.", e);
    }
  }

  @Async
  public void sendApplicantResultEmail(String from, ApplicantEmailMessage message) {
    String subject =
        message.getAccepted()
            ? String.format("[%s] %s 전형 합격 안내", message.getCompanyName(), message.getPositionName())
            : String.format(
                "[%s] %s 전형 결과 안내", message.getCompanyName(), message.getPositionName());

    // 타임리프 컨텍스트에 변수 세팅
    Context context = new Context();
    context.setVariable("applicantName", message.getApplicantName());
    context.setVariable("companyName", message.getCompanyName());
    context.setVariable("positionName", message.getPositionName());
    context.setVariable("isAccepted", message.getAccepted());

    String htmlContent = templateEngine.process("applicant-result-email", context);
    sendHtmlEmail(from, message.getApplicantEmail(), subject, htmlContent);
  }

  @Async
  public void sendWorkspaceVerificationEmail(
      String from, WorkspaceVerificationEmailMessage message) {
    String subject = "[CalFit] 워크스페이스 인증을 완료해 주세요";
    String link = message.getVerificationUrl() + "?token=" + message.getVerificationToken();

    Context context = new Context();
    context.setVariable("userName", message.getUserName());
    context.setVariable("verificationLink", link);
    context.setVariable("expiresAt", message.getExpiresAt()); // 만료 시각 추가

    String htmlContent = templateEngine.process("workspace-verification-email", context);
    sendHtmlEmail(from, message.getUserEmail(), subject, htmlContent);
  }
}
