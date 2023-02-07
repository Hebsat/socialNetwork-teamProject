package socialnet.service;

import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import socialnet.api.request.LikeRq;
import socialnet.api.response.CommonRs;
import socialnet.api.response.LikeRs;
import socialnet.errors.NotFoundException;
import socialnet.kafka.NotificationsKafkaProducer;
import socialnet.model.entities.Like;
import socialnet.model.entities.Person;
import socialnet.model.entities.interfaces.Liked;
import socialnet.repository.CommentsRepository;
import socialnet.repository.LikesRepository;
import socialnet.repository.PostsRepository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LikesService {

    private final LikesRepository likesRepository;
    private final PostsRepository postsRepository;
    private final CommentsRepository commentsRepository;
    private final NotificationsService notificationsService;
    private  final PersonCacheService personCacheService;
    private final NotificationsKafkaProducer notificationsKafkaProducer;

    @Value("${socialNetwork.timezone}")
    private String timezone;

    public CommonRs<LikeRs> putLike(LikeRq likeRq) throws NotFoundException {
        Person person = personCacheService.getPersonByContext();
        Liked liked = getLikedEntity(likeRq.getItemId(), likeRq.getType());
        if (getLikeFromCurrentPerson(person, liked) != null) {
            throw new NotFoundException("Like is already exists");
        }
        Like like = new Like();
        like.setEntity(liked);
        like.setAuthor(person);
        like.setTime(LocalDateTime.now(ZoneId.of(timezone)));
        likesRepository.save(like);
        if (like.getEntity().getAuthor().getPersonSettings().getLikeNotification()) {
            notificationsService.sendNotificationToWs(like, liked.getAuthor());
            notificationsKafkaProducer.sendMessage(like, liked.getAuthor());
            notificationsService.sendNotificationToTelegramBot(like, liked.getAuthor());
        }
        return getLikesResponse(liked);
    }

    private CommonRs<LikeRs> getLikesResponse(Liked liked) {
        List<Like> likes = likesRepository.findLikesByEntity(liked.getType(), liked);
        List<Long> users = likes.stream().map(like -> like.getAuthor().getId()).collect(Collectors.toList());
        LikeRs likeRs = LikeRs.builder()
                .likes(likes.size())
                .users(users)
                .build();
        return buildCommonResponse(likeRs);
    }

    public CommonRs<LikeRs> getLikesResponse(long entityId, String type) throws NotFoundException {
        return getLikesResponse(getLikedEntity(entityId, type));
    }

    public CommonRs<LikeRs> deleteLike(long entityId, String type) throws NotFoundException {
        Person person = personCacheService.getPersonByContext();
        Liked liked = getLikedEntity(entityId, type);
        Like like = getLikeFromCurrentPerson(person, liked);
        if (like != null) {
            notificationsService.deleteNotification(like);
            likesRepository.delete(like);
        }
        return getLikesResponse(liked);
    }

    private Like getLikeFromCurrentPerson(Person person, Liked liked) {
        return likesRepository.findLikeByPersonAndEntity(liked.getType(), liked, person).orElse(null);
    }

    private Liked getLikedEntity(long entityId, String type) throws NotFoundException {
        Liked liked;
        switch (type) {
            case "Post" : {
                liked = postsRepository.findById(entityId).orElseThrow();
                break;
            }
            case "Comment" : {
                liked = commentsRepository.findById(entityId).orElseThrow();
                break;
            }
            default: throw new NotFoundException(type + " with id " + entityId + " not found!");
        }
        return liked;
    }

    @Named("getLikesCount")
    public Integer getLikesCount(Liked liked) {
        return likesRepository.findLikesByEntity(liked.getType(), liked).size();
    }

    @Named("getMyLike")
    public Boolean getMyLike(Liked liked) {
        Person person = personCacheService.getPersonByContext();
        Like like = likesRepository.findLikeByPersonAndEntity(liked.getType(), liked, person).orElse(null);
        return like != null && person.getId().equals(like.getAuthor().getId());
    }

    private CommonRs<LikeRs> buildCommonResponse(LikeRs likeResponse) {
        return CommonRs.<LikeRs>builder()
                .timestamp(System.currentTimeMillis())
                .data(likeResponse)
                .build();
    }
}
