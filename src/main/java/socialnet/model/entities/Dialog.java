package socialnet.model.entities;

import lombok.*;

import javax.persistence.*;
import java.time.ZonedDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "dialogs")
public class Dialog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "last_active_time")
    private ZonedDateTime lastActiveTime;

    @OneToOne
    @JoinColumn(name = "last_message_id")
    private Message lastMessage;

    @ManyToOne
    @JoinColumn(name = "first_person_id", nullable = false)
    private Person firstPerson;

    @ManyToOne
    @JoinColumn(name = "second_person_id", nullable = false)
    private Person secondPerson;

    @Column(name = "is_deleted", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isDeleted;

    @OneToMany(mappedBy = "dialog", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Message> messages;

    public Dialog(Person firstPerson, Person secondPerson, ZonedDateTime lastActiveTime, Boolean isDeleted) {
        this.firstPerson = firstPerson;
        this.secondPerson = secondPerson;
        this.lastActiveTime = lastActiveTime;
        this.isDeleted = isDeleted;
    }
}
