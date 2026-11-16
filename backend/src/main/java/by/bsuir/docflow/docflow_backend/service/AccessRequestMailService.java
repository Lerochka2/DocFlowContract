package by.bsuir.docflow.docflow_backend.service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@Service
public class AccessRequestMailService {
    private final JavaMailSender sender;
    private final String from;
    public AccessRequestMailService(JavaMailSender sender,@Value("${spring.mail.username:}") String from) {
        this.sender=sender; this.from=from;
    }
    public void send(String recipient,String subject,String body) {
        if(from.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Отправка email не настроена. Решение не сохранено.");
        var message=new SimpleMailMessage(); message.setFrom(from); message.setTo(recipient);
        message.setSubject(subject); message.setText(body);
        try { sender.send(message); }
        catch(MailException e) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Не удалось отправить письмо. Решение не сохранено, повторите позже."); }
    }
}
