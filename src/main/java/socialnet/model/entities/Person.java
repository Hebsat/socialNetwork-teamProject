package socialnet.model.entities;

import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import socialnet.model.entities.interfaces.Notificationed;
import socialnet.model.enums.FriendshipStatusTypes;
import socialnet.model.enums.MessagePermissionTypes;
import socialnet.model.enums.NotificationTypes;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "persons", indexes = {
        @Index(name = "first_name_index", columnList = "first_name"),
        @Index(name = "last_name_index", columnList = "last_name"),
        @Index(name = "email_index", columnList = "email", unique = true)})
public class Person implements Notificationed, Serializable {
    private static final long serialVersionUID = -4439114469417994311L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "reg_date", nullable = false)
    private LocalDateTime regDate;

    @Column(name = "birth_date")
    private LocalDateTime birthDate;

    @Column(nullable = false)
    private String email;

    private String phone;

    private String password;

    private String photo;

    private String about;

    private String city;

    private String country;

    @Column(name = "confirmation_code")
    private Integer confirmationCode;

    @Column(name = "is_approved", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isApproved;

    @Column (name = "message_permission", nullable = false, columnDefinition = "VARCHAR(255) DEFAULT 'ALL'")
    @Enumerated(EnumType.STRING)
    private MessagePermissionTypes messagePermission;

    @Column(name = "last_online_time")
    private LocalDateTime lastOnlineTime;

    @Column(name = "is_blocked", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isBlocked;

    @Column (name = "change_password_token")
    private String changePasswordToken;

    @Column (name = "notifications_session_id")
    private String notificationSessionID;

    @Column (name = "online_status")
    private String onlineStatus;

    @Column(name = "is_deleted", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isDeleted;

    @Column(name = "deleted_time")
    private LocalDateTime deletedTime;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "friendships",
            joinColumns = @JoinColumn(name = "src_person_id"),inverseJoinColumns = @JoinColumn(name = "dst_person_id"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<Person> srcFriendships = new ArrayList<>();

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "friendships",
            joinColumns = @JoinColumn(name = "dst_person_id"), inverseJoinColumns = @JoinColumn(name = "src_person_id"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<Person> dstFriendships = new ArrayList<>();

    @OneToMany(mappedBy = "person", cascade = CascadeType.ALL)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<BlockHistory> blockHistoryList = new ArrayList<>();

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<Post> posts = new ArrayList<>();

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<Comment> comments = new ArrayList<>();

    @OneToOne
    @JoinColumn(name = "person_settings_id", referencedColumnName = "id", nullable = false)
    private PersonSettings personSettings;

    @OneToMany(mappedBy = "person", cascade = CascadeType.ALL)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<Notification> notifications = new ArrayList<>();

    @OneToMany(mappedBy = "firstPerson", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<Dialog> firstPersonDialogs = new ArrayList<>();

    @OneToMany(mappedBy = "secondPerson", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<Dialog> secondPersonDialogs = new ArrayList<>();

    @OneToMany(mappedBy = "author", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<Message> messages = new ArrayList<>();

    @OneToMany(mappedBy = "author", cascade = CascadeType.REMOVE)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<Like> likes = new ArrayList<>();

    @Transient
    private FriendshipStatusTypes friendStatus = FriendshipStatusTypes.UNKNOWN;

    @Column(name = "telegram_id")
    private Long telegramId;

    @Override
    public NotificationTypes getNotificationType() {
        return NotificationTypes.FRIEND_BIRTHDAY;
    }

    @Override
    public Person getAuthor() {
        return this;
    }

    @Override
    public String getSimpleInfo() {
        return String.valueOf(LocalDateTime.now().getYear() - birthDate.getYear());
    }

    @Override
    public String toString() {
        return "Person{" +
                "id=" + id +
                ", name='" + firstName + ' ' + lastName +
                "', email='" + email +
                "', isBlocked=" + isBlocked +
                ", isDeleted=" + isDeleted +
                '}';
    }

    public Person(Long id, String email) {
        this.id = id;
        this.email = email;
    }

    public Person(Long id) {
        this.id = id;
    }
}
