package by.bsuir.docflow.docflow_backend.controller;
import by.bsuir.docflow.docflow_backend.service.AccessRequestService;
import by.bsuir.docflow.docflow_backend.entity.AccessRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
public class AccessRequestController {
    private final AccessRequestService service;
    private final String supportEmail;
    private final Map<String,Deque<Long>> attempts=new HashMap<>();
    public AccessRequestController(AccessRequestService service,@Value("${app.support.email:${spring.mail.username:}}") String supportEmail) {
        this.service=service; this.supportEmail=supportEmail;
    }
    @PostMapping("/api/public/access-requests") @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> submit(@RequestBody Map<String,String> data,HttpServletRequest request) {
        throttle(request.getRemoteAddr()); return service.submit(data);
    }
    @GetMapping("/api/public/access-requests/options")
    public List<AccessRequestService.DepartmentOption> options() { return service.options(); }
    @GetMapping("/api/public/support") public Map<String,String> support() { return Map.of("email",supportEmail); }
    @GetMapping("/api/admin/access-requests") public List<AccessRequest> list() { return service.list(); }
    @GetMapping("/api/admin/access-requests/count") public Map<String,Long> count() { return Map.of("count",service.count()); }
    @PostMapping("/api/admin/access-requests/{id}/approve")
    public AccessRequest approve(@PathVariable Long id,@RequestBody Map<String,String> data,@AuthenticationPrincipal Jwt jwt) {
        return service.approve(id,((Number)jwt.getClaim("userId")).longValue(),data.get("login"),data.get("roleCode"));
    }
    @PostMapping("/api/admin/access-requests/{id}/reject")
    public AccessRequest reject(@PathVariable Long id,@RequestBody Map<String,String> data,@AuthenticationPrincipal Jwt jwt) {
        return service.reject(id,((Number)jwt.getClaim("userId")).longValue(),data.get("reason"));
    }
    private synchronized void throttle(String address) {
        long now=System.currentTimeMillis(),cutoff=now-900_000;
        attempts.values().forEach(q -> { while(!q.isEmpty() && q.peekFirst()<cutoff) q.removeFirst(); });
        attempts.entrySet().removeIf(e -> e.getValue().isEmpty());
        var queue=attempts.computeIfAbsent(address,key -> new ArrayDeque<>());
        if(queue.size()>=5) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Слишком много заявок. Повторите через 15 минут.");
        queue.addLast(now);
    }
}
