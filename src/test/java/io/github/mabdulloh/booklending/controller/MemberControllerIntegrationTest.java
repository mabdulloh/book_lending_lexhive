package io.github.mabdulloh.booklending.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mabdulloh.booklending.dto.member.CreateMemberRequest;
import io.github.mabdulloh.booklending.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class MemberControllerIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper om;

    @Test
    @DisplayName("create - admin - 201")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void create_admin_ok() throws Exception {
        var req = new CreateMemberRequest("Alice", "alice@example.com", "alice12345");
        mvc.perform(post("/api/v1/members").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uuid").exists());
    }

    @Test
    @DisplayName("create - member role - 403")
    @WithMockUser(username = "member", roles = "MEMBER")
    void create_memberForbidden() throws Exception {
        mvc.perform(post("/api/v1/members").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new CreateMemberRequest("X", "x@example.com", "x1234567"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("create - invalid email - 400")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void create_invalidEmail() throws Exception {
        mvc.perform(post("/api/v1/members").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new CreateMemberRequest("Bob", "not-email", "bob12345"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("create - duplicate email - 409")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void create_duplicateEmail() throws Exception {
        var req = new CreateMemberRequest("Alice", "alice2@example.com", "alice12345");
        mvc.perform(post("/api/v1/members").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/members").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("list - admin - 200")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void list_ok() throws Exception {
        mvc.perform(get("/api/v1/members"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("list - member - 403")
    @WithMockUser(username = "member", roles = "MEMBER")
    void list_memberForbidden() throws Exception {
        mvc.perform(get("/api/v1/members"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("get - admin - 200")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void get_ok() throws Exception {
        UUID uuid = createMember("carol@example.com");
        mvc.perform(get("/api/v1/members/{uuid}", uuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("carol@example.com"));
    }

    @Test
    @DisplayName("get - missing - 404")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void get_missing() throws Exception {
        mvc.perform(get("/api/v1/members/{uuid}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("delete - admin - 204 then GET 404 (soft delete)")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void delete_softDelete() throws Exception {
        UUID uuid = createMember("dan@example.com");
        mvc.perform(delete("/api/v1/members/{uuid}", uuid).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/members/{uuid}", uuid))
                .andExpect(status().isNotFound());
    }

    private UUID createMember(String email) throws Exception {
        var req = new CreateMemberRequest("Test", email, "test12345");
        String json = mvc.perform(post("/api/v1/members").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(om.readTree(json).get("uuid").asText());
    }
}