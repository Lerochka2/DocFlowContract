package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.repository.*;
import by.bsuir.docflow.docflow_backend.scheduler.ApprovalSlaScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.mockito.ArgumentCaptor;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties={"app.jwt.secret=YWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWE=","spring.jpa.show-sql=false"})
class AccessRequestIntegrationTest {
    @Autowired AccessRequestService service;
    @Autowired AccessRequestRepository requests;
    @Autowired AppUserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean AccessRequestMailService mail;
    @MockitoBean ApprovalSlaScheduler scheduler;
    Map<String,String> data(String suffix) { return Map.of("fullName","Проверка заявки Интеграционная","email",suffix+"@example.invalid","department","Юридический отдел","position","Юрист","comment","Автоматическая проверка с откатом данных"); }
    Long adminId(){return users.findByLogin("admin").orElseThrow().getId();}
    @Test @Transactional void approvalPersistsUserAndPasswordMatchesMail() {
        String login="access_it_"+System.nanoTime();
        Long id=(Long)service.submit(data(login)).get("requestId");
        assertFalse(users.existsByEmailIgnoreCase(login+"@example.invalid"));
        assertTrue(jdbc.queryForObject("select count(*) from notifications where dedup_key=?",Long.class,"access-request-"+id)>0);
        service.approve(id,adminId(),login,"LAWYER");
        var user=users.findByLogin(login).orElseThrow();
        var body=ArgumentCaptor.forClass(String.class);verify(mail).send(eq(user.getEmail()),anyString(),body.capture());
        String password=body.getValue().split("Временный пароль: ")[1].split("\n")[0];
        assertTrue(encoder.matches(password,user.getPasswordHash()));
        assertEquals("LAWYER",user.getRole().getCode());
        assertEquals("APPROVED",requests.findById(id).orElseThrow().status);
        assertThrows(ResponseStatusException.class,()->service.approve(id,adminId(),login,"LAWYER"));
    }
    @Test @Transactional void rejectionDoesNotCreateUserAndDuplicateIsRejected() {
        String login="access_it_"+System.nanoTime();Long id=(Long)service.submit(data(login)).get("requestId");
        assertThrows(ResponseStatusException.class,()->service.submit(data(login)));
        service.reject(id,adminId(),"Доступ не согласован руководителем");
        assertEquals("REJECTED",requests.findById(id).orElseThrow().status);
        assertFalse(users.existsByEmailIgnoreCase(login+"@example.invalid"));
    }
    @Test void mailFailureRollsBackUserAndDecision() {
        String login="access_it_"+System.nanoTime();Long id=(Long)service.submit(data(login)).get("requestId");
        try {
            doThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Test mail failure")).when(mail).send(anyString(),anyString(),anyString());
            assertThrows(ResponseStatusException.class,()->service.approve(id,adminId(),login,"LAWYER"));
            assertFalse(users.existsByLogin(login));
            assertEquals("NEW",requests.findById(id).orElseThrow().status);
        } finally {
            jdbc.update("delete from notifications where dedup_key=?","access-request-"+id);
            requests.deleteById(id);
        }
    }
}
