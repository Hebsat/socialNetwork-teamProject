package socialnet.service;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import socialnet.api.request.FindPersonRq;
import socialnet.api.request.UserRq;
import socialnet.api.response.CommonRs;
import socialnet.api.response.ComplexRs;
import socialnet.api.response.PersonRs;
import socialnet.api.response.StorageDataRs;
import socialnet.errors.EmptyFieldException;
import socialnet.errors.FileException;
import socialnet.mappers.PersonMapper;
import socialnet.model.entities.Person;
import socialnet.repository.*;
import socialnet.service.search.SearchPersons;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@EnableScheduling
public class UsersService {
    private final static int MAX_IMAGE_LENGTH = 1024000;
    @Value("${user.time-to-delete}")
    long timeToDel;

    private final BlockHistoriesRepository blockHistoriesRepository;
    private final CommentsRepository commentsRepository;
    private final DialogsRepository dialogsRepository;
    private final FriendshipsRepository friendshipsRepository;
    private final LikesRepository likesRepository;
    private final MessagesRepository messagesRepository;
    private final NotificationsRepository notificationsRepository;
    private final PostsRepository postsRepository;
    private final PersonsRepository personsRepository;
    private final PersonSettingsRepository personSettingsRepository;
    private final CloudinaryService cloudinaryService;
    private final GeolocationsService geolocationsService;
    private final PersonMapper personMapper;
    private final SearchPersons searchPersons;
    private final PersonCacheService personCacheService;

    public CommonRs<StorageDataRs> storeImage(MultipartFile photo) throws IOException, FileException {
        Person person = personCacheService.getPersonByEmail(SecurityContextHolder.getContext().getAuthentication().getName());
        long personId = person.getId();
        StorageDataRs dataRs = new StorageDataRs();

        String originFileName = Objects.requireNonNull(photo.getOriginalFilename());
        String extension = originFileName.substring(originFileName.lastIndexOf(".") + 1);
        String newFileName = "image" + personId + "." + extension;
        String newFileNameShort = "image" + personId;
        if (!extension.equals("jpg") && !extension.equals("png")) {
            throw new FileException("The file is not in the correct format. Requires 'jpg' or 'png'");
        }
        long fileLength = photo.getSize();
        if (fileLength > MAX_IMAGE_LENGTH) {
            throw new FileException("The maximum file size must not exceed 1 MB");
        }

        dataRs.setBytes(0);
        dataRs.setCreatedAt(0);

        BufferedImage bufferedImage = ImageIO.read(photo.getInputStream());
        File outputfile = new File(newFileName);
        ImageIO.write(bufferedImage, extension, outputfile);

        cloudinaryService.uploadImage(outputfile);
        outputfile.delete();
        String relativePath = cloudinaryService.getImage(newFileNameShort);
        dataRs.setId(String.valueOf(personId));
        dataRs.setOwnerId(personId);
        dataRs.setFileName(newFileName);
        dataRs.setRelativeFilePath(relativePath);
        dataRs.setFileFormat(photo.getContentType());
        dataRs.setFileType(extension);

        person.setPhoto(relativePath);
        personCacheService.cachePerson(person);
        return CommonRs.<StorageDataRs>builder()
                .timestamp(System.currentTimeMillis())
                .data(dataRs)
                .build();
    }

    public CommonRs<PersonRs> editProfile(UserRq userRq) throws Exception {
        Person person = personCacheService.getPersonByEmail(SecurityContextHolder.getContext().getAuthentication().getName());
        PersonRs personRs = personMapper.toPersonResponse(person);
        if (userRq.getCity() != null && userRq.getCountry() == null) {
            throw new EmptyFieldException("Не указана страна");
        }
        if (userRq.getAbout() != null) {
            person.setAbout(userRq.getAbout());
            personRs.setAbout(userRq.getAbout());
        }
        if (userRq.getBirth_date() != null) {
            person.setBirthDate(LocalDateTime.from(OffsetDateTime.parse(userRq.getBirth_date())));
            personRs.setBirthDate(LocalDateTime.from(OffsetDateTime.parse(userRq.getBirth_date())));
        }
        person.setCity(userRq.getCity());
        personRs.setCity(userRq.getCity());
        if (userRq.getCity() != null) {
            geolocationsService.setCityGismeteoId(userRq.getCity());
        }
        person.setCountry(userRq.getCountry());
        personRs.setCountry(userRq.getCountry());
        if (userRq.getFirst_name() != null) {
            person.setFirstName(userRq.getFirst_name());
            personRs.setFirstName(userRq.getFirst_name());
        }
        if (userRq.getLast_name() != null) {
            person.setLastName(userRq.getLast_name());
            personRs.setLastName(userRq.getLast_name());
        }
        if (userRq.getPhone() != null) {
            person.setPhone(userRq.getPhone());
            personRs.setPhone(userRq.getPhone());
        }
        if (userRq.getPhoto_id() != null) {
            person.setPhoto(userRq.getPhoto_id());
            personRs.setPhoto(userRq.getPhoto_id());
        }

        personCacheService.cachePerson(person);
        return CommonRs.<PersonRs>builder()
                .timestamp(System.currentTimeMillis())
                .data(personRs)
                .build();
    }

    public CommonRs<List<PersonRs>> findPersons(FindPersonRq personRq, int offset, int perPage) throws EmptyFieldException {
        if (personRq.getFirst_name() == null && personRq.getLast_name() == null && personRq.getAge_from() == null
                && personRq.getAge_to() == null && personRq.getCity() == null && personRq.getCountry() == null) {
            throw new EmptyFieldException("All fields in query are empty");
        }
        Page<Person> persons = searchPersons.findPersons(personRq, offset, perPage);
        return buildCommonResponse(offset, perPage, persons.getContent(), persons.getTotalElements());
    }

    private CommonRs<List<PersonRs>> buildCommonResponse(int offset, int perPAge, List<Person> persons, long total) {
        return CommonRs.<List<PersonRs>>builder()
                .timestamp(System.currentTimeMillis())
                .data(personsToResponse(persons))
                .offset(offset)
                .perPage(perPAge)
                .total(total)
                .build();
    }

    public CommonRs<ComplexRs> deleteProfile() {
        Person person = personsRepository.findPersonByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).get();
        person.setIsDeleted(true);
        person.setDeletedTime(LocalDateTime.now());
        personCacheService.cachePerson(person);

        return CommonRs.<ComplexRs>builder()
                .timestamp(System.currentTimeMillis())
                .data(ComplexRs.builder().build())
                .build();
    }

    public CommonRs<ComplexRs> recoverProfile() {
        Person person = personsRepository.findPersonByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).get();
        person.setIsDeleted(false);
        person.setDeletedTime(null);
        personCacheService.cachePerson(person);

        return CommonRs.<ComplexRs>builder()
                .timestamp(System.currentTimeMillis())
                .data(ComplexRs.builder().build())
                .build();
    }

    @Scheduled(fixedRateString = "${user.time-to-delete}")
    public void executeOldDeletes() {
        List<Long> idToDelete = personsRepository.idToDelete(timeToDel);
        for (long oldId : idToDelete) {
            blockHistoriesRepository.deleteAll(blockHistoriesRepository.findBHtoDelete(oldId));
            friendshipsRepository.deleteAll(friendshipsRepository.findFriendsToDelete(oldId));
            postsRepository.deleteAll(postsRepository.findPostsToDelete(oldId));
            List<Long> commentsIdToDelete = commentsRepository.findCommentsIdToDelete(oldId);
            commentsRepository.commentsDelete(oldId);
            for (long oldCommentId : commentsIdToDelete) {
                commentsRepository.secondaryCommentsDelete(oldCommentId);
            }
            personSettingsRepository.persSetDelete(oldId);
            dialogsRepository.dialogsDelete(oldId);
            notificationsRepository.notificationDelete(oldId);
            messagesRepository.messagesDelete(oldId);
            likesRepository.likeDelete(oldId);
            personsRepository.personDelete(oldId);
        }
    }

    private List<PersonRs> personsToResponse(List<Person> persons) {
        return persons.stream().map(personMapper::toPersonResponse).collect(Collectors.toList());
    }
}
