package io.github.mabdulloh.booklending.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mabdulloh.booklending.dto.book.CreateBookRequest;
import io.github.mabdulloh.booklending.dto.loan.BorrowRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class LoanControllerIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper om;

    @Test
    @DisplayName("borrow - member - 201, then return 200, availableCopies restored")
    @WithMockUser(username = "member", roles = "MEMBER")
    void borrow_and_return() throws Exception {
        UUID bookUuid = createBook("9780132350890", 2);
        UUID memberUuid = createMember("eve@example.com");

        BorrowRequest req = new BorrowRequest(bookUuid, memberUuid);
        String loanJson = mvc.perform(post("/api/v1/loans").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookUuid").value(bookUuid.toString()))
                .andExpect(jsonPath("$.returnedAt").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        UUID loanUuid = UUID.fromString(om.readTree(loanJson).get("uuid").asText());

        mvc.perform(post("/api/v1/loans/{uuid}/return", loanUuid)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnedAt").exists());

        mvc.perform(get("/api/v1/books/{uuid}", bookUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableCopies").value(2));
    }

    @Test
    @DisplayName("borrow - book unavailable - 422")
    @WithMockUser(username = "member", roles = "MEMBER")
    void borrow_bookUnavailable() throws Exception {
        UUID bookUuid = createBook("9780132350891", 0);
        UUID memberUuid = createMember("frank@example.com");

        mvc.perform(post("/api/v1/loans").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new BorrowRequest(bookUuid, memberUuid))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("borrow - admin role - 403")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void borrow_adminForbidden() throws Exception {
        UUID bookUuid = createBook("9780132350892", 1);
        UUID memberUuid = createMember("grace@example.com");

        mvc.perform(post("/api/v1/loans").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new BorrowRequest(bookUuid, memberUuid))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("borrow - max active loans - 422")
    @WithMockUser(username = "member", roles = "MEMBER")
    void borrow_maxExceeded() throws Exception {
        UUID memberUuid = createMember("hank@example.com");

        for (int i = 0; i < 3; i++) {
            UUID bookUuid = createBook("978013235090" + i, 1);
            mvc.perform(post("/api/v1/loans").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(new BorrowRequest(bookUuid, memberUuid))))
                    .andExpect(status().isCreated());
        }

        UUID extraBook = createBook("9780132350999", 1);
        mvc.perform(post("/api/v1/loans").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new BorrowRequest(extraBook, memberUuid))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("list by member - admin - 200")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void listByMember_ok() throws Exception {
        UUID memberUuid = createMember("ivy@example.com");
        mvc.perform(get("/api/v1/loans").param("memberId", memberUuid.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("overdue - admin - 200")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void overdue_admin_ok() throws Exception {
        mvc.perform(get("/api/v1/loans/overdue"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("overdue - member - 403")
    @WithMockUser(username = "member", roles = "MEMBER")
    void overdue_memberForbidden() throws Exception {
        mvc.perform(get("/api/v1/loans/overdue"))
                .andExpect(status().isForbidden());
    }

    private UUID createBook(String isbn, int copies) throws Exception {
        var req = new CreateBookRequest("The Technological Republic", "Alexander C Karp", isbn, copies);
        String json = mvc.perform(post("/api/v1/books").with(csrf())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(om.readTree(json).get("uuid").asText());
    }

    private UUID createMember(String email) throws Exception {
        var req = new CreateMemberRequest("Jonathan Pierce", email, "jon12345");
        String json = mvc.perform(post("/api/v1/members").with(csrf())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(om.readTree(json).get("uuid").asText());
    }
}