package socialnet.service;

import lombok.RequiredArgsConstructor;
import socialnet.api.response.RegionStatisticRs;
import socialnet.errors.EmptyFieldException;
import socialnet.model.entities.Post;
import socialnet.repository.*;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StatisticService {

    private final PersonsRepository personsRepository;
    private final CitiesRepository citiesRepository;
    private final CountriesRepository countriesRepository;
    private final DialogsRepository dialogsRepository;
    private final LikesRepository likesRepository;
    private final MessagesRepository messagesRepository;
    private final PostsRepository postsRepository;
    private final TagsRepository tagsRepository;
    private final CommentsRepository commentsRepository;

    public Long getCountUsers() {
        return personsRepository.count();
    }

    public Long getCountUsersByCountry(String country) throws EmptyFieldException {
        if (country == null) {
            throw new EmptyFieldException("param 'country' is empty");
        }
        return personsRepository.countAllByCountryIgnoreCase(country);
    }

    public Long getCountUsersByCity(String city) throws EmptyFieldException {
        if (city == null) {
            throw new EmptyFieldException("param 'city' is empty");
        }
        return personsRepository.countAllByCityIgnoreCase(city);
    }

    public Long getCountCities() {
        return citiesRepository.count();
    }

    public List<RegionStatisticRs> getCitiesWithCountUsers() {
        return personsRepository.getCityWithUsersCount();
    }

    public Long getCountCountries() {
        return countriesRepository.count();
    }

    public List<RegionStatisticRs> getCountriesWithCountUsers() {
        return personsRepository.getCountryWithUsersCount();
    }

    public Long getCountDialogs() {
        return dialogsRepository.count();
    }

    public Long getCountDialogsByUserId(Long userId) throws EmptyFieldException {
        if (userId == null) {
            throw new EmptyFieldException("param 'userId' is empty");
        }
        return dialogsRepository.countAllByFirstPersonIdAndIsDeletedFalseOrSecondPersonIdAndIsDeletedFalse(userId, userId);
    }

    public Long getCountLikes() {
        return likesRepository.count();
    }

    public Long getCountLikesByEntityId(Long entityId) throws EmptyFieldException {
        if (entityId == null) {
            throw new EmptyFieldException("param 'entityId' is empty");
        }
        return likesRepository.countAllByEntityId(entityId);
    }

    public Long getCountMessages() {
        return messagesRepository.count();
    }

    public Long getCountMessagesByDialogId(Long dialogId) throws EmptyFieldException {
        if (dialogId == null) {
            throw new EmptyFieldException("param 'dialogId' is empty");
        }
        return messagesRepository.countAllByDialogId(dialogId);
    }

    public Map<String, Long> getCountMessagesByTwoUsers(Long firstUserId, Long SecondUserId) throws EmptyFieldException {
        if (firstUserId == null || SecondUserId == null) {
            throw new EmptyFieldException("param 'firstUserId' or 'SecondUserId' is empty");
        }
        Map<String, Long> dialogMessages = new HashMap<>();
        dialogMessages.put(
                "author id - " + firstUserId + ", recipient - " + SecondUserId, messagesRepository.countAllByAuthorIdAndRecipientId(firstUserId, SecondUserId));
        dialogMessages.put(
                "author id - " + SecondUserId + ", recipient - " + firstUserId, messagesRepository.countAllByAuthorIdAndRecipientId(SecondUserId, firstUserId)
        );
        return dialogMessages;
    }

    public Long getCountPosts() {
        return postsRepository.count();
    }

    public Long getCountPostsByUserId(Long userId) throws EmptyFieldException {
        if (userId == null) {
            throw new EmptyFieldException("param 'userId' is empty");
        }
        return postsRepository.countAllByAuthorId(userId);
    }

    public Long getCountCommentsByPostId(Long postId) throws EmptyFieldException {
        if (postId == null) {
            throw new EmptyFieldException("param 'postId' is empty");
        }
        return commentsRepository.countAllByPostId(postId);
    }

    public Long getCountTags() {
        return tagsRepository.count();
    }

    public Long getCountTagsByPostId(Long postId) throws EmptyFieldException {
        if (postId == null) {
            throw new EmptyFieldException("param 'postId' is empty");
        }
        Optional<Post> post = postsRepository.findById(postId);
        if (post.isPresent()) {
            return (long) post.get().getTags().size();
        } else {
            return null;
        }
    }
}
