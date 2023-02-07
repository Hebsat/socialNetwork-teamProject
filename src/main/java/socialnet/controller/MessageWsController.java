package socialnet.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.*;
import socialnet.api.websocket.MessageCommonWs;
import socialnet.api.websocket.MessageTypingWs;
import socialnet.api.websocket.MessageWs;
import org.springframework.stereotype.Controller;
import socialnet.service.MessageWsService;

@Controller
@RequiredArgsConstructor
public class MessageWsController {

    private final MessageWsService messageWsService;

    @MessageMapping("/dialogs/send_message")
    public void sendMessage(@Payload MessageWs messageWs) throws Exception {
        messageWsService.postMessage(messageWs);
    }

    @MessageMapping("/dialogs/start_typing")
    public void startTyping(@Header("dialog_id") Long dialogId, @Payload MessageTypingWs messageTypingWs) {
        messageWsService.messageTypingFromWs(dialogId, messageTypingWs);
    }

    @MessageMapping("/dialogs/stop_typing")
    public void stopTyping(@Header("dialog_id") Long dialogId, @Payload MessageTypingWs messageTypingWs) {
        messageWsService.messageTypingFromWs(dialogId, messageTypingWs);
    }

    @MessageMapping("/dialogs/edit_message")
    public void editMessage(@Payload MessageCommonWs message) throws Exception {
        messageWsService.changeMessage(message);
    }

    @MessageMapping("/dialogs/delete_messages")
    public void deleteMessages(@Payload MessageCommonWs messages) throws Exception {
        messageWsService.removeMessage(messages);
    }

    @MessageMapping("/dialogs/recover_message")
    public void recoverMessage(@Payload MessageCommonWs messageCommonWs) throws Exception {
        messageWsService.restoreMessage(messageCommonWs);
    }

    @MessageMapping("/dialogs/close_dialog")
    public void closeDialog(@Payload MessageCommonWs messageCommonWs) throws Exception {
        messageWsService.closeDialog(messageCommonWs);
    }
}
