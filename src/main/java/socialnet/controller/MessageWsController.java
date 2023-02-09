package socialnet.controller;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.*;
import socialnet.api.response.ErrorRs;
import socialnet.api.websocket.MessageCommonWs;
import socialnet.api.websocket.MessageTypingWs;
import socialnet.api.websocket.MessageWs;
import org.springframework.stereotype.Controller;
import socialnet.service.MessageWsService;

@Controller
@RequiredArgsConstructor
public class MessageWsController {

    private final MessageWsService messageWsService;

    @ApiOperation(value = "send message by WebSocket")
    @ApiImplicitParam(name = "authorization", value = "Access Token", required = true, paramType = "header", dataTypeClass = String.class, example = "JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "400", description = "\"Name of error\"",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorRs.class))}),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @MessageMapping("/dialogs/send_message")
    public void sendMessage(@Payload MessageWs messageWs) throws Exception {
        messageWsService.postMessage(messageWs);
    }

    @ApiOperation(value = "send start typing for message status by WebSocket")
    @ApiImplicitParam(name = "authorization", value = "Access Token", required = true, paramType = "header", dataTypeClass = String.class, example = "JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "400", description = "\"Name of error\"",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorRs.class))}),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @MessageMapping("/dialogs/start_typing")
    public void startTyping(@Header("dialog_id") Long dialogId, @Payload MessageTypingWs messageTypingWs) {
        messageWsService.messageTypingFromWs(dialogId, messageTypingWs);
    }

    @ApiOperation(value = "send stop typing for message by WebSocket")
    @ApiImplicitParam(name = "authorization", value = "Access Token", required = true, paramType = "header", dataTypeClass = String.class, example = "JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "400", description = "\"Name of error\"",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorRs.class))}),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @MessageMapping("/dialogs/stop_typing")
    public void stopTyping(@Header("dialog_id") Long dialogId, @Payload MessageTypingWs messageTypingWs) {
        messageWsService.messageTypingFromWs(dialogId, messageTypingWs);
    }

    @ApiOperation(value = "edit message by WebSocket")
    @ApiImplicitParam(name = "authorization", value = "Access Token", required = true, paramType = "header", dataTypeClass = String.class, example = "JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "400", description = "\"Name of error\"",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorRs.class))}),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @MessageMapping("/dialogs/edit_message")
    public void editMessage(@Payload MessageCommonWs message) throws Exception {
        messageWsService.changeMessage(message);
    }

    @ApiOperation(value = "delete message by WebSocket")
    @ApiImplicitParam(name = "authorization", value = "Access Token", required = true, paramType = "header", dataTypeClass = String.class, example = "JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "400", description = "\"Name of error\"",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorRs.class))}),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @MessageMapping("/dialogs/delete_messages")
    public void deleteMessages(@Payload MessageCommonWs messages) throws Exception {
        messageWsService.removeMessage(messages);
    }

    @ApiOperation(value = "recover message by WebSocket")
    @ApiImplicitParam(name = "authorization", value = "Access Token", required = true, paramType = "header", dataTypeClass = String.class, example = "JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "400", description = "\"Name of error\"",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorRs.class))}),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @MessageMapping("/dialogs/recover_message")
    public void recoverMessage(@Payload MessageCommonWs messageCommonWs) throws Exception {
        messageWsService.restoreMessage(messageCommonWs);
    }

    @ApiOperation(value = "close dialog session with user and clear deleted messages by WebSocket")
    @ApiImplicitParam(name = "authorization", value = "Access Token", required = true, paramType = "header", dataTypeClass = String.class, example = "JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "400", description = "\"Name of error\"",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorRs.class))}),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @MessageMapping("/dialogs/close_dialog")
    public void closeDialog(@Payload MessageCommonWs messageCommonWs) throws Exception {
        messageWsService.closeDialog(messageCommonWs);
    }
}
