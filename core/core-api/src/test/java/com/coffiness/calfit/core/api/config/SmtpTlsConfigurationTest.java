package com.coffiness.calfit.core.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;

class SmtpTlsConfigurationTest {

  @Test
  void actualMailSessionUsesDefaultTrustAndRequiresVerifiedStartTls() throws IOException {
    mailContext()
        .run(
            context -> {
              assertThat(context).hasNotFailed().hasBean("mailSender");
              BeanWrapperImpl sender = new BeanWrapperImpl(context.getBean("mailSender"));
              assertThat(sender.getPropertyValue("host")).isEqualTo("smtp.resend.com");
              assertThat(sender.getPropertyValue("port")).isEqualTo(587);
              assertThat(sender.getPropertyValue("defaultEncoding")).isEqualTo("UTF-8");
              Properties properties = (Properties) sender.getPropertyValue("session.properties");
              assertSecureTls(properties);
              assertThat(properties)
                  .containsEntry("mail.smtp.connectiontimeout", "5000")
                  .containsEntry("mail.smtp.timeout", "5000")
                  .containsEntry("mail.smtp.writetimeout", "5000");
            });
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "unused.invalid"})
  void obsoleteTrustEnvironmentVariableDoesNotReachJavaMail(String legacyValue) throws IOException {
    mailContext("SPRING_MAIL_SMTP_SSL_TRUST=" + legacyValue)
        .run(
            context -> {
              assertThat(context).hasNotFailed().hasBean("mailSender");
              BeanWrapperImpl sender = new BeanWrapperImpl(context.getBean("mailSender"));
              assertSecureTls((Properties) sender.getPropertyValue("session.properties"));
            });
  }

  @Test
  void smtpTimeoutEnvironmentOverridesRemainAvailable() throws IOException {
    mailContext(
            "SPRING_MAIL_SMTP_CONNECTION_TIMEOUT=7000",
            "SPRING_MAIL_SMTP_TIMEOUT=8000",
            "SPRING_MAIL_SMTP_WRITE_TIMEOUT=9000")
        .run(
            context -> {
              assertThat(context).hasNotFailed().hasBean("mailSender");
              BeanWrapperImpl sender = new BeanWrapperImpl(context.getBean("mailSender"));
              Properties properties = (Properties) sender.getPropertyValue("session.properties");
              assertSecureTls(properties);
              assertThat(properties)
                  .containsEntry("mail.smtp.connectiontimeout", "7000")
                  .containsEntry("mail.smtp.timeout", "8000")
                  .containsEntry("mail.smtp.writetimeout", "9000");
            });
  }

  private static void assertSecureTls(Properties properties) {
    assertThat(properties)
        .doesNotContainKeys(
            "mail.smtp.ssl.trust",
            "mail.smtp.ssl.socketFactory",
            "mail.smtp.ssl.socketFactory.class",
            "mail.smtp.socketFactory.class")
        .containsEntry("mail.smtp.ssl.checkserveridentity", "true")
        .containsEntry("mail.smtp.auth", "true")
        .containsEntry("mail.smtp.starttls.enable", "true")
        .containsEntry("mail.smtp.starttls.required", "true")
        .containsEntry("mail.smtp.ssl.enable", "false")
        .containsEntry("mail.smtp.ssl.protocols", "TLSv1.2");
  }

  private static ApplicationContextRunner mailContext(String... overrides) throws IOException {
    var yaml =
        new YamlPropertySourceLoader()
            .load("calfit-mail-configuration", new ClassPathResource("application.yml"))
            .get(0);
    return new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(MailSenderAutoConfiguration.class))
        .withInitializer(context -> context.getEnvironment().getPropertySources().addLast(yaml))
        .withPropertyValues(
            "SPRING_MAIL_HOST=smtp.resend.com",
            "SPRING_MAIL_PORT=587",
            "SPRING_MAIL_USERNAME=synthetic-smtp-user",
            "SPRING_MAIL_PASSWORD=synthetic-test-password",
            "SPRING_MAIL_SMTP_AUTH=true",
            "SPRING_MAIL_SMTP_STARTTLS_ENABLE=true",
            "SPRING_MAIL_SMTP_STARTTLS_REQUIRED=true",
            "SPRING_MAIL_SMTP_SSL_ENABLE=false",
            "SPRING_MAIL_SMTP_SSL_PROTOCOLS=TLSv1.2",
            "SPRING_MAIL_SMTP_CONNECTION_TIMEOUT=5000",
            "SPRING_MAIL_SMTP_TIMEOUT=5000",
            "SPRING_MAIL_SMTP_WRITE_TIMEOUT=5000")
        .withPropertyValues(overrides);
  }
}
