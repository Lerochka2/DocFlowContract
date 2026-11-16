package by.bsuir.docflow.docflow_backend.service;
import by.bsuir.docflow.docflow_backend.entity.AccessRequest;
import by.bsuir.docflow.docflow_backend.entity.enums.NotificationType;
import by.bsuir.docflow.docflow_backend.repository.*;
import by.bsuir.docflow.docflow_backend.dto.user.CreateUserRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;
import java.time.LocalDateTime;
import java.security.SecureRandom;
import java.util.*;

@Service
public class AccessRequestService {
    public record PositionOption(String name, String roleCode) {}
    public record DepartmentOption(String department, List<PositionOption> positions) {}
    private static final List<DepartmentOption> OPTIONS = List.of(
        new DepartmentOption("Отдел делопроизводства", List.of(new PositionOption("Делопроизводитель", "DOCUMENT_OPERATOR"))),
        new DepartmentOption("Юридический отдел", List.of(new PositionOption("Юрист", "LAWYER"))),
        new DepartmentOption("Договорной отдел", List.of(new PositionOption("Менеджер по договорам", "CONTRACT_MANAGER")))
    );
    public List<DepartmentOption> options() { return OPTIONS; }
    private String roleFor(String department, String position) {
        return OPTIONS.stream().filter(d -> d.department().equals(department))
            .flatMap(d -> d.positions().stream()).filter(p -> p.name().equals(position))
            .map(PositionOption::roleCode).findFirst().orElse(null);
    }
    private final AccessRequestRepository repository;
    private final AppUserRepository users;
    private final UserAdminService admin;
    private final AccessRequestMailService mail;
    private final NotificationService notifications;
    public AccessRequestService(AccessRequestRepository repository,AppUserRepository users,UserAdminService admin,AccessRequestMailService mail,NotificationService notifications) {
        this.repository=repository; this.users=users; this.admin=admin; this.mail=mail; this.notifications=notifications;
    }
    @Transactional
    public Map<String,Object> submit(Map<String,String> data) {
        if(!Set.of("fullName","email","department","position","comment").containsAll(data.keySet())) bad("Форма содержит недопустимые поля");
        var r=new AccessRequest();
        r.fullName=text(data.get("fullName"),3,255,"ФИО");
        r.email=text(data.get("email"),5,255,"Электронная почта").toLowerCase(Locale.ROOT);
        if(!r.email.matches("^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$")) bad("Некорректный адрес электронной почты");
        r.department=text(data.get("department"),1,255,"Подразделение");
        r.position=text(data.get("position"),1,255,"Должность");
        if(roleFor(r.department,r.position)==null) bad("Выберите должность из списка выбранного подразделения");
        r.comment=text(data.get("comment"),0,1000,"Комментарий");
        if(users.existsByEmailIgnoreCase(r.email) || repository.existsByEmailAndStatus(r.email,"NEW"))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Для этого адреса уже существует заявка или учётная запись. Обратитесь в поддержку.");
        try { repository.saveAndFlush(r); }
        catch(DataIntegrityViolationException e) { throw new ResponseStatusException(HttpStatus.CONFLICT,"Заявка для этого адреса уже существует"); }
        for(var user:users.findByRoleCodeAndEnabledTrue("ADMIN")) notifications.createNotification(user,null,NotificationType.ACCESS_REQUEST_CREATED,
            "Новая заявка на доступ",r.fullName+" — "+r.department,"/access-requests","Рассмотреть заявку","access-request-"+r.id);
        return Map.of("requestId",r.id,"status","NEW","message","Заявка успешно отправлена");
    }
    @Transactional(readOnly=true) public List<AccessRequest> list() { return repository.findAllByOrderByCreatedAtDesc(); }
    @Transactional(readOnly=true) public long count() { return repository.countByStatus("NEW"); }
    @Transactional
    public AccessRequest approve(Long id,Long adminId,String login,String role) {
        if(role==null || !Set.of("DOCUMENT_OPERATOR","LAWYER","CONTRACT_MANAGER").contains(role)) bad("Выберите рабочую роль сотрудника");
        login=text(login,3,100,"Логин");
        if(!login.matches("[a-zA-Z0-9._-]{3,100}")) bad("Логин: латинские буквы, цифры, точка, дефис и подчёркивание");
        var r=pending(id);
        String expectedRole=roleFor(r.department,r.position);
        // Older requests used free text; administrators can still process those requests.
        if(expectedRole!=null && !expectedRole.equals(role)) bad("Роль должна соответствовать подразделению и должности в заявке");
        byte[] bytes=new byte[18]; new SecureRandom().nextBytes(bytes);
        String password="Df9!"+Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        var user=admin.createUser(new CreateUserRequest(login,r.fullName,r.email,password,role));
        r.status="APPROVED"; r.createdUserId=user.userId(); finish(r,adminId);
        repository.flush();
        // Mail failure rolls back both the decision and the newly created account.
        mail.send(r.email,"DocFlow — заявка одобрена","Здравствуйте, "+r.fullName+"!\n\nВаша заявка #"+r.id+" одобрена.\nЛогин: "+user.login()+"\nВременный пароль: "+password+"\n\nПосле входа измените пароль в настройках DocFlow.\nНе передавайте данные для входа другим лицам.");
        return r;
    }
    @Transactional
    public AccessRequest reject(Long id,Long adminId,String reason) {
        reason=text(reason,3,1000,"Причина отказа");
        var r=pending(id); r.status="REJECTED"; r.adminComment=reason; finish(r,adminId); repository.flush();
        mail.send(r.email,"DocFlow — результат рассмотрения заявки","Здравствуйте, "+r.fullName+"!\n\nЗаявка #"+r.id+" отклонена.\nПричина: "+reason+"\n\nПри необходимости обратитесь к администратору DocFlow.");
        return r;
    }
    private AccessRequest pending(Long id) {
        var r=repository.findLockedById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Заявка не найдена"));
        if(!"NEW".equals(r.status)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Заявка уже рассмотрена");
        return r;
    }
    private void finish(AccessRequest r,Long adminId) { r.processedByUserId=adminId; r.processedAt=LocalDateTime.now(); }
    private static String text(String value,int min,int max,String name) {
        String result=value==null?"":value.trim();
        if(result.length()<min || result.length()>max) bad(name+": допустимая длина от "+min+" до "+max+" символов");
        return result;
    }
    private static void bad(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,message); }
}
