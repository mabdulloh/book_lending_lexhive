package io.github.mabdulloh.booklending.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mabdulloh.booklending.dto.book.CreateBookRequest;
import io.github.mabdulloh.booklending.dto.book.UpdateBookRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class BookControllerIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper om;

    @Test
    @DisplayName("create - admin - 201")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void create_admin_ok() throws Exception {
        var req = new CreateBookRequest("Clean Code", "R Martin", "9780132350884", 5);

        mvc.perform(post("/api/v1/books").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uuid").exists())
                .andExpect(jsonPath("$.availableCopies").value(5));
    }

    @Test
    @DisplayName("create - member - 403")
    @WithMockUser(username = "member", roles = "MEMBER")
    void create_memberForbidden() throws Exception {
        mvc.perform(post("/api/v1/books").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new CreateBookRequest("T", "A", "123", 1))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("create - validation - 400")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void create_validation() throws Exception {
        mvc.perform(post("/api/v1/books").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new CreateBookRequest("", "", "", 0))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("create - duplicate isbn - 409")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void create_duplicateIsbn() throws Exception {
        var req = new CreateBookRequest("Rich Dad Poor Dad", "Robert Kiyosaki", "9780132350885", 1);
        mvc.perform(post("/api/v1/books").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/books").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("list - member - 200")
    @WithMockUser(username = "member", roles = "MEMBER")
    void list_ok() throws Exception {
        mvc.perform(get("/api/v1/books"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("get - missing - 404")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void get_missing() throws Exception {
        mvc.perform(get("/api/v1/books/{uuid}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("get - 200 by uuid")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void get_ok() throws Exception {
        UUID uuid = createBook("9780132350886", 3);
        mvc.perform(get("/api/v1/books/{uuid}", uuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(uuid.toString()));
    }

    @Test
    @DisplayName("update - admin - 200")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void update_ok() throws Exception {
        UUID uuid = createBook("9780132350887", 3);
        mvc.perform(put("/api/v1/books/{uuid}", uuid).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new UpdateBookRequest("New Title", null, 5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New Title"))
                .andExpect(jsonPath("$.totalCopies").value(5));
    }

    @Test
    @DisplayName("delete - admin - 204 then GET 404 (soft delete)")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void delete_softDelete() throws Exception {
        UUID uuid = createBook("9780132350888", 1);
        mvc.perform(delete("/api/v1/books/{uuid}", uuid).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/books/{uuid}", uuid))
                .andExpect(status().isNotFound());
    }

    private UUID createBook(String isbn, int copies) throws Exception {
        var req = new CreateBookRequest("Factfulness", "Hans Rosling", isbn, copies);
        String json = mvc.perform(post("/api/v1/books").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(om.readTree(json).get("uuid").asText());
    }
}