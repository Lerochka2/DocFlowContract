package by.bsuir.docflow.docflow_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PasswordVerificationMailService {

    private final JavaMailSender mailSender;

    private final String senderEmail;


    public PasswordVerificationMailService(
            JavaMailSender mailSender,

            @Value(
                    "${spring.mail.username:}"
            )
            String senderEmail
    ) {
        this.mailSender =
                mailSender;

        this.senderEmail =
                senderEmail;
    }


    public void sendVerificationCode(
            String recipientEmail,
            String recipientName,
            String code
    ) {

        if (
                senderEmail == null
                ||
                senderEmail.isBlank()
        ) {

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Отправка email пока не настроена"
            );
        }

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setFrom(
                senderEmail
        );

        message.setTo(
                recipientEmail
        );

        message.setSubject(
                "DocFlow — код подтверждения"
        );

        message.setText(
                "Здравствуйте, "
                        + recipientName
                        + "!\n\n"
                        + "Код подтверждения смены пароля:\n\n"
                        + code
                        + "\n\n"
                        + "Код действует 5 минут.\n"
                        + "Если вы не запрашивали смену пароля, "
                        + "просто проигнорируйте это письмо.\n\n"
                        + "DocFlow"
        );

        try {

            mailSender.send(
                    message
            );

        } catch (MailException exception) {

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Не удалось отправить код подтверждения",
                    exception
            );
        }
    }
}