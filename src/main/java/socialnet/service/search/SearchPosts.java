package socialnet.service.search;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import socialnet.api.request.FindPostRq;
import socialnet.model.entities.Friendship;
import socialnet.model.entities.Person;
import socialnet.model.entities.Post;
import socialnet.model.enums.FriendshipStatusTypes;
import socialnet.repository.FriendshipsRepository;
import socialnet.repository.PersonsRepository;
import socialnet.repository.PostsRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static socialnet.service.search.PostSpecification.*;

@Component
@RequiredArgsConstructor
public class SearchPosts {

    private final PostsRepository postsRepository;

    private final PersonsRepository personsRepository;

    private final FriendshipsRepository friendshipsRepository;

    private final CommonSearchMethods commonSearchMethods;

    public Page<Post> findPosts(FindPostRq postRq, int offset, int perPage) {
        List<Person> personsWhoBLockedMe = getPersonsWhoBlockedMe();
        Pageable pageable = PageRequest.of(offset, perPage);
        Specification<Post> specification = textLike(postRq.getText()).and(isBlockedFalse());
        if (personsWhoBLockedMe.size() != 0) {
            specification = specification.and(excludeBlockedPosts(personsWhoBLockedMe));
        }
        if (postRq.getTags() != null) {
            specification = specification.and(tagsContains(postRq.getTags()));
        }
        if (postRq.getDate_to() != null || postRq.getDate_from() != null) {
            specification = specification.and(datesBetween(commonSearchMethods.longToLocalDateTime(postRq.getDate_from()),
                            commonSearchMethods.longToLocalDateTime(postRq.getDate_to())));
        }
        if (postRq.getAuthor() != null) {
            List<Person> persons = commonSearchMethods.findPersonByNameAndLastNameContains(postRq.getAuthor());
            specification = specification.and(postAuthorsIs(persons));
        }
        return postsRepository.findAll(specification, pageable);
    }

    private List<Person> getPersonsWhoBlockedMe() {
        Person me = personsRepository.findPersonByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).get();
        List<Friendship> friendships = friendshipsRepository.findFriendshipsByDstPersonIdAndFriendshipStatus(me.getId(), FriendshipStatusTypes.BLOCKED);
        List<Person> personsWhoBLockedMe = new ArrayList<>();
        friendships.forEach(friendship -> personsWhoBLockedMe.add(friendship.getSrcPerson()));
        return personsWhoBLockedMe;
    }
}
