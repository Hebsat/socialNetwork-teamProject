package socialnet.service;

import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.stereotype.Service;
import socialnet.model.entities.Dialog;
import socialnet.model.entities.Message;
import socialnet.model.enums.ReadStatusTypes;
import socialnet.repository.MessagesRepository;

@Service
@RequiredArgsConstructor
public class DialogMapService {

    private final MessagesRepository messagesRepository;
    private final PersonCacheService personCacheService;

    @Named("getUnreadMessagesCountForDialog")
    public Long getUnreadMessagesCountForDialog (Dialog dialog) {
        return messagesRepository.findAllByRecipientAndIsDeletedFalse(personCacheService.getPersonByContext()).stream()
                .filter(m -> m.getDialog().getId().equals(dialog.getId()) &&
                        m.getReadStatus().equals(ReadStatusTypes.SENT))
                .count();
    }

    @Named("isAuthor")
    public Boolean isAuthor(Message message) {
        return message.getAuthor().getId().equals(personCacheService.getPersonByContext().getId());
    }
}
