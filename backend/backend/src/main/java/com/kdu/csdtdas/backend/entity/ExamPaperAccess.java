package com.kdu.csdtdas.backend.entity;import jakarta.persistence.*;import java.time.*;
@Entity @Table(name="exam_paper_access",uniqueConstraints=@UniqueConstraint(columnNames={"paper_id","user_id"}))
public class ExamPaperAccess{@Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
@ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="paper_id") private ExamPaper paper;
@ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="user_id") private User user;
@ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="granted_by_id") private User grantedBy;
@Column(nullable=false) private LocalDateTime grantedAt;private LocalDateTime expiresAt;private LocalDateTime revokedAt;
public ExamPaperAccess(){}@PrePersist void init(){grantedAt=LocalDateTime.now();}
public Long getId(){return id;}public ExamPaper getPaper(){return paper;}public void setPaper(ExamPaper v){paper=v;}public User getUser(){return user;}public void setUser(User v){user=v;}
public User getGrantedBy(){return grantedBy;}public void setGrantedBy(User v){grantedBy=v;}public LocalDateTime getGrantedAt(){return grantedAt;}public LocalDateTime getExpiresAt(){return expiresAt;}public void setExpiresAt(LocalDateTime v){expiresAt=v;}public LocalDateTime getRevokedAt(){return revokedAt;}public void setRevokedAt(LocalDateTime v){revokedAt=v;}
}