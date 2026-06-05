package com.example.MyPickCafe.entity;

import com.example.MyPickCafe.domain.NotificationType;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter; import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity @Getter @Setter @NoArgsConstructor
@Table(name="notification", indexes=@Index(columnList="recipient_id,created_at"))
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="recipient_id", nullable=false)
    private Member recipient;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="cafe_id")
    private Cafe cafe;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="review_id")
    private Review review;

    @Enumerated(EnumType.STRING) @Column(length=32, nullable=false)
    private NotificationType type;

    @Column(length=300)
    private String message;

    @Column(nullable=false)
    private boolean read = false;

    @Column(nullable=false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
