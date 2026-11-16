package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.entity.AccessRequest;
import by.bsuir.docflow.docflow_backend.repository.*;
import by.bsuir.docflow.docflow_backend.dto.user.UserResponse;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccessRequestServiceTest {
    AccessRequestRepository repository = mock(AccessRequestRepository.class);
    AppUserRepository users = mock(AppUserRepository.class);
    UserAdminService admin = mock(UserAdminService.class);
    AccessRequestMailService mail = mock(AccessRequestMailService.class);
    NotificationService notifications = mock(NotificationService.class);
    AccessRequestService service = new AccessRequestService(repository, users, admin, mail, notifications);
    AccessRequest request;
    @BeforeEach void setup() {
        request = new AccessRequest(); request.id = 7L; request.fullName = "Иванов Иван Иванович";
        request.email = "ivanov@example.invalid"; request.status = "NEW";
        request.department="Юридический отдел"; request.position="Юрист";
        when(repository.findLockedById(7L)).thenReturn(Optional.of(request));
    }
    @Test void submissionDoesNotCreateUser() {
        when(repository.saveAndFlush(any())).thenAnswer(i -> { AccessRequest r = i.getArgument(0); r.id=8L; return r; });
        var result=service.submit(Map.of("fullName"," Иванов Иван Иванович ","email"," IVANOV@example.invalid ","department","Юридический отдел","position","Юрист"));
        assertEquals("NEW",result.get("status")); verifyNoInteractions(admin,mail);
        verify(repository).saveAndFlush(argThat(r -> r.email.equals("ivanov@example.invalid") && r.fullName.equals("Иванов Иван Иванович")));
    }
    @Test void mismatchedDepartmentAndPositionAreRejected() {
        assertThrows(ResponseStatusException.class, () -> service.submit(Map.of(
            "fullName","Иванов Иван Иванович","email","ivanov@example.invalid",
            "department","Юридический отдел","position","Делопроизводитель")));
        verifyNoInteractions(repository, admin, mail);
    }
    @Test void unknownDepartmentAndPositionAreRejected() {
        assertThrows(ResponseStatusException.class, () -> service.submit(Map.of(
            "fullName","Иванов Иван Иванович","email","ivanov@example.invalid",
            "department","Произвольный отдел","position","Администратор")));
        verifyNoInteractions(repository, admin, mail);
    }
    @Test void approvalRoleMustMatchSelectedPosition() {
        assertThrows(ResponseStatusException.class, () -> service.approve(7L,1L,"ivanov","DOCUMENT_OPERATOR"));
        verifyNoInteractions(admin,mail);
    }
    @Test void publicRoleInjectionIsRejected() {
        assertThrows(ResponseStatusException.class, () -> service.submit(Map.of("role","ADMIN")));
        verifyNoInteractions(repository,admin);
    }
    @Test void approvalCannotGrantAdmin() {
        assertThrows(ResponseStatusException.class, () -> service.approve(7L,1L,"ivanov","ADMIN"));
        verifyNoInteractions(admin,mail);
    }
    @Test void processedRequestCannotBeApprovedAgain() {
        request.status="REJECTED";
        assertThrows(ResponseStatusException.class, () -> service.approve(7L,1L,"ivanov","LAWYER"));
        verifyNoInteractions(admin,mail);
    }
    @Test void approvalCreatesUserWithGeneratedPasswordAndSendsMail() {
        when(admin.createUser(any())).thenReturn(new UserResponse(9L,"ivanov",request.fullName,request.email,"LAWYER","Юрист",true,null,null,null));
        service.approve(7L,1L,"ivanov","LAWYER");
        verify(admin).createUser(argThat(r -> r.password().length()>=16 && r.roleCode().equals("LAWYER") && r.email().equals(request.email)));
        verify(mail).send(eq(request.email),anyString(),contains("ivanov"));
        assertEquals("APPROVED",request.status); assertEquals(9L,request.createdUserId);
    }
    @Test void rejectionRequiresReasonAndDoesNotCreateAccount() {
        assertThrows(ResponseStatusException.class, () -> service.reject(7L,1L,"  "));
        service.reject(7L,1L,"Нет согласования руководителя");
        assertEquals("REJECTED",request.status); verifyNoInteractions(admin);
        verify(mail).send(eq(request.email),anyString(),contains("Нет согласования"));
    }
    @Test void invalidOrOversizedFieldsAreRejected() {
        var data=new HashMap<>(Map.of("fullName","Иванов", "email","bad", "department","Отдел", "position","Юрист"));
        assertThrows(ResponseStatusException.class, () -> service.submit(data));
        data.put("email","a@example.invalid"); data.put("comment","x".repeat(1001));
        assertThrows(ResponseStatusException.class, () -> service.submit(data)); verifyNoInteractions(admin);
    }
}
