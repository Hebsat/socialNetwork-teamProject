package socialnet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import socialnet.api.response.CommonRs;
import socialnet.api.response.PersonRs;
import socialnet.mappers.PersonMapper;
import socialnet.model.entities.Friendship;
import socialnet.model.entities.Person;
import socialnet.model.enums.FriendshipStatusTypes;
import socialnet.repository.FriendshipsRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonsService {

    private final FriendshipsRepository friendshipsRepository;
    private final PersonMapper personMapper;
    private final FriendsService friendsService;
    private final PersonCacheService personCacheService;

    public CommonRs<PersonRs> getPersonDataById(Long id) {
        Person srcPerson = personCacheService.getPersonByEmail(SecurityContextHolder.getContext().getAuthentication().getName());
        Person person = personCacheService.getPersonById(id);
        person.setFriendStatus(friendsService.getStatusTwoPersons(person, srcPerson));
        if (blockUserPage(srcPerson, id)) {
            return buildCommoRsForBlockedPage(person);
        }
        return getCommonPersonResponse(person);
    }

    public CommonRs<PersonRs> getMyData() {
        return getCommonPersonResponse(personCacheService.getPersonByEmail(SecurityContextHolder.getContext().getAuthentication().getName()));
    }


    public boolean validatePerson(Person person) {
        return person != null && person.equals(personCacheService.getPersonByContext());
    }

    private CommonRs<PersonRs> getCommonPersonResponse(Person person) {
        return CommonRs.<PersonRs>builder()
                .timestamp(System.currentTimeMillis())
                .data(personMapper.toPersonResponse(person))
                .build();
    }

    private boolean blockUserPage(Person me, Long userWhoBlocked) {
        List<Friendship> friendships = friendshipsRepository.findFriendshipsByDstPersonIdAndFriendshipStatus(me.getId(), FriendshipStatusTypes.BLOCKED);
        List<Long> srcPersonsIds = getSrcPersons(friendships);
        return srcPersonsIds.contains(userWhoBlocked);
    }

    private List<Long> getSrcPersons (List<Friendship> friendships) {
        List<Long> ids = new ArrayList<>();
        friendships.forEach(friendship -> ids.add(friendship.getSrcPerson().getId()));
        return ids;
    }

    private CommonRs<PersonRs> buildCommoRsForBlockedPage(Person person) {
        PersonRs personWhoBlockedMe = personMapper.toPersonResponse(new Person());
        personWhoBlockedMe.setIsBlockedByCurrentUser(true);
        personWhoBlockedMe.setId(person.getId());
        personWhoBlockedMe.setFirstName(person.getFirstName());
        personWhoBlockedMe.setLastName(person.getLastName());
        personWhoBlockedMe.setFriendStatus(person.getFriendStatus());
        return CommonRs.<PersonRs>builder()
                .timestamp(System.currentTimeMillis())
                .data(personWhoBlockedMe)
                .build();
    }
}
