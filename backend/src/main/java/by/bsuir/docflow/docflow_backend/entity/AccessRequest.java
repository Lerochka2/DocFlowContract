package by.bsuir.docflow.docflow_backend.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name="access_requests")
public class AccessRequest {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(name="full_name",nullable=false) public String fullName;
    @Column(nullable=false) public String email;
    @Column(nullable=false) public String department;
    @Column(nullable=false) public String position;
    @Column(nullable=false,length=1000) public String comment = "";
    @Column(nullable=false,length=16) public String status = "NEW";
    @Column(name="admin_comment",length=1000) public String adminComment;
    @Column(name="processed_by_user_id") public Long processedByUserId;
    @Column(name="created_user_id") public Long createdUserId;
    @Column(name="created_at",nullable=false) public LocalDateTime createdAt = LocalDateTime.now();
    @Column(name="processed_at") public LocalDateTime processedAt;
}
