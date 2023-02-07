package socialnet.controller;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import socialnet.aop.annotations.UpdateOnlineTime;
import socialnet.api.request.DialogUserShortListDto;
import socialnet.api.response.*;
import socialnet.service.DialogsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dialogs")
@RequiredArgsConstructor
@Tag(name = "dialogs-controller", description = "Get dialogs, start dialog, get read and unread messages")
public class DialogsController {

    private final DialogsService dialogsService;

    @GetMapping
    @ApiOperation(value = "recover comment by id")
    @ApiImplicitParam(name = "authorization", value = "Access Token", required = true, paramType = "header", dataTypeClass = String.class, example = "JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "400", description = "\"Name of error\"",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorRs.class))}),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @UpdateOnlineTime
    public CommonRs<List<DialogRs>> dialogs() throws Exception {
        return dialogsService.getAllDialogs();
    }

    @PostMapping
    @ApiOperation(value = "start dialog with user")
    @ApiImplicitParam(name = "authorization", value = "Access Token", required = true, paramType = "header", dataTypeClass = String.class, example = "JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "400", description = "\"Name of error\"",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorRs.class))}),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @UpdateOnlineTime
    public CommonRs<ComplexRs> dialogsStart(@RequestBody DialogUserShortListDto dialogUserShortListDto) throws Exception {
        return dialogsService.beginDialog(dialogUserShortListDto);
    }

    @GetMapping("/{dialogId}/messages")
    @ApiOperation(value = "get messages from dialog")
    @ApiImplicitParam(name = "authorization", value = "Access Token", required = true, paramType = "header", dataTypeClass = String.class, example = "JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "400", description = "\"Name of error\"",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorRs.class))}),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @UpdateOnlineTime
    public CommonRs<List<MessageRs>> messages(@PathVariable Long dialogId) {
        return dialogsService.getMessages(dialogId);
    }

    @GetMapping("/unreaded")
    @ApiOperation(value = "get count of unread messages")
    @ApiImplicitParam(name = "authorization", value = "Access Token", required = true, paramType = "header", dataTypeClass = String.class, example = "JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "400", description = "\"Name of error\"",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorRs.class))}),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @UpdateOnlineTime
    public CommonRs<ComplexRs> unread() {
        return dialogsService.getUnreadMessages();
    }

    @PutMapping("/{dialogId}/read")
    @ApiOperation(value = "read messages in dialog")
    @ApiImplicitParam(name = "authorization", value = "Access Token", required = true, paramType = "header", dataTypeClass = String.class, example = "JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "400", description = "\"Name of error\"",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorRs.class))}),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @UpdateOnlineTime
    public CommonRs<ComplexRs> read(@PathVariable Long dialogId) {
        return dialogsService.setReadMessages(dialogId);
    }
}
