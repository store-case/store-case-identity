package com.leedahun.storecaseidentity.common.mail;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.leedahun.storecaseidentity.domain.auth.exception.EmailSendFailedException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class EmailClientTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailClient emailClient;

    @BeforeEach
    void setUp() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    @DisplayName("이메일 전송 성공 시 mailSender.send()가 호출된다")
    void sendOneEmail_success() throws Exception {
        // given
        String to = "test@example.com";
        String subject = "테스트 제목";
        String text = "<p>테스트 본문</p>";

        // when
        emailClient.sendOneEmail(to, subject, text);

        // then
        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    @DisplayName("MessagingException 발생 시 예외가 발생한다")
    void sendOneEmail_messagingException() throws Exception {
        // given
        String to = "error@example.com";
        String subject = "에러 발생";
        String text = "<p>본문</p>";

        when(mailSender.createMimeMessage()).thenThrow(new EmailSendFailedException());

        // when
        assertThrows(EmailSendFailedException.class, () -> emailClient.sendOneEmail(to, subject, text));

        // then
        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }
}