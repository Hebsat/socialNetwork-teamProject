package main.model.entities;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "friendships")
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "status_id", nullable = false)
    private FriendshipStatus friendshipStatus;

    @JoinColumn(name = "sent_time", nullable = false)
    private LocalDateTime sentTime;

    @ManyToOne
    @JoinColumn(name = "src_person_id", nullable = false)
    private Person srcPerson;

    @ManyToOne
    @JoinColumn(name = "dst_person_id", nullable = false)
    private Person dstPerson;
}