package com.leedahun.storecaseidentity.common.mail;

import com.leedahun.storecaseidentity.domain.auth.exception.EmailSendFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailClient {

    private final JavaMailSender mailSender;

    public void sendOneEmail(String to, String subject, String text) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();

            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);
            mimeMessageHelper.setTo(to);
            mimeMessageHelper.setSubject(subject);
            mimeMessageHelper.setText(text, true);

            mailSender.send(mimeMessage);

        } catch (MessagingException e){
            log.error("Error sending email", e);
            throw new EmailSendFailedException();
        }
    }

}
