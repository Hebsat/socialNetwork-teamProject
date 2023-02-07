package socialnet.controller;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import socialnet.model.enums.FriendshipStatusTypes;
import socialnet.repository.FriendshipsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.RefreshMode.BEFORE_EACH_TEST_METHOD;
import static socialnet.model.enums.FriendshipStatusTypes.REQUEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureEmbeddedDatabase(refresh = BEFORE_EACH_TEST_METHOD)
@Sql("/test-dataANN.sql")
@AutoConfigureMockMvc
@WithUserDetails("rhoncus.nullam@yahoo.edu")
class FriendsControllerTest {

    @Autowired
    FriendshipsRepository friendshipsRepository;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TokenCheck tokenCheck;

    @Test
    void getOutgoingRequests() throws Exception {
        Integer expectedOutgoingRequests = 1;
        mockMvc.perform(get("/api/v1/friends/outgoing_requests")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.size()").value(expectedOutgoingRequests));
    }

    @Test
    void userBlocksUser() throws Exception {
        Integer expectedNumberFriendsAfterBlocking = 0;
        Integer expectedNumberBlockedFriendship = 1;
        mockMvc.perform(post("/api/v1/friends/block_unblock/2")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is2xxSuccessful());
        mockMvc.perform(get("/api/v1/friends")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.size()").value(expectedNumberFriendsAfterBlocking));
        Integer actualNumberBlockedFriendship = friendshipsRepository.findFriendshipByFriendshipStatus(FriendshipStatusTypes.BLOCKED).size();
        assertEquals(expectedNumberBlockedFriendship, actualNumberBlockedFriendship);
    }

    @Test
    void sendFriendshipRequest() throws Exception {
        Integer expectedNumberFriendshipsAfterSending = 8;
        mockMvc.perform(post("/api/v1/friends/5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is2xxSuccessful());
        assertEquals(expectedNumberFriendshipsAfterSending, friendshipsRepository.findAll().size());
    }

    @Test
    void addFriend() throws Exception {
        Integer expectedNumberFriendsAfterAdding = 2;
        mockMvc.perform(post("/api/v1/friends/request/3")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is2xxSuccessful());
        mockMvc.perform(get("/api/v1/friends")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.size()").value(expectedNumberFriendsAfterAdding));
    }

    @Test
    void getPotentialFriends() throws Exception {
        Integer expectedNumberPotentialFriends = 1;
        mockMvc.perform(get("/api/v1/friends/request")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.size()").value(expectedNumberPotentialFriends));
    }

    @Test
    void getFriends() throws Exception {
        Integer expectedNumberFriends = 1;
        mockMvc.perform(get("/api/v1/friends")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.size()").value(expectedNumberFriends));
    }

    @Test
    void deleteFriend() throws Exception {
        Integer expectedNumberFriendsAfterDeleting = 0;
        mockMvc.perform(delete("/api/v1/friends/2"))
                .andDo(print())
                .andExpect(status().is2xxSuccessful());
        mockMvc.perform(get("/api/v1/friends")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.size()").value(expectedNumberFriendsAfterDeleting));
    }

    @Test
    void deleteSentRequest() throws Exception {
        Integer expectedNumberFriendships = 4;
        Integer expectedNumberFriendshipsWithRequest = 1;
        mockMvc.perform(delete("/api/v1/friends/request/4"))
                .andDo(print())
                .andExpect(status().is2xxSuccessful());
        Integer actualNumberFriendshipsWithRequest = Math.toIntExact(friendshipsRepository.findAll().
                stream().filter(fs -> fs.getFriendshipStatus() == REQUEST).count());
        assertEquals(expectedNumberFriendships, friendshipsRepository.findAll().size());
        assertEquals(expectedNumberFriendshipsWithRequest, actualNumberFriendshipsWithRequest);
    }

    @Test
    void getRecommendedFriends() throws Exception {
        Integer expectedNumberRecommendedFriends = 1;
        Integer offsetPage = 0;
        Integer outputNumberPerPage = 8;
        String url = "/api/v1/friends/recommendations";
        mockMvc.perform(get(url))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.total").value(expectedNumberRecommendedFriends))
                .andExpect(jsonPath("$.offset").value(offsetPage))
                .andExpect(jsonPath("$.perPage").value(outputNumberPerPage))
                .andExpect(jsonPath("$.data").isArray());

        tokenCheck.wrongOrExpiredTokenCheck(mockMvc, HttpMethod.GET, url);
    }
}
