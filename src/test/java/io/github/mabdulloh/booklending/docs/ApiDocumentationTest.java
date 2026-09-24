package io.github.mabdulloh.booklending.docs;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mabdulloh.booklending.dto.auth.LoginRequest;
import io.github.mabdulloh.booklending.dto.book.CreateBookRequest;
import io.github.mabdulloh.booklending.dto.book.UpdateBookRequest;
import io.github.mabdulloh.booklending.dto.loan.BorrowRequest;
import io.github.mabdulloh.booklending.dto.member.CreateMemberRequest;
import io.github.mabdulloh.booklending.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@IntegrationTest
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
class ApiDocumentationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper om;
    @Autowired private RestDocumentationContextProvider restDocumentation;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        RestDocumentationResultHandler document = document("{method-name}", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()));
        this.mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .apply(documentationConfiguration(restDocumentation))
                .alwaysDo(document)
                .build();
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void auth_login() throws Exception {
        var req = new LoginRequest("admin", "admin123");
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andDo(document("auth/login",
                        requestFields(
                                fieldWithPath("username").type(JsonFieldType.STRING).description("Login username"),
                                fieldWithPath("password").type(JsonFieldType.STRING).description("Plaintext password")),
                        responseFields(
                                fieldWithPath("token").type(JsonFieldType.STRING).description("Signed JWT"))));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void books_create() throws Exception {
        var req = new CreateBookRequest("Clean Code", "Robert Martin", "9780132350884" + UUID.randomUUID().toString().substring(0, 6), 5);
        mvc.perform(post("/api/v1/books").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(req)))
                .andDo(document("books/create",
                        requestFields(
                                fieldWithPath("title").type(JsonFieldType.STRING).description("Book title"),
                                fieldWithPath("author").type(JsonFieldType.STRING).description("Author name"),
                                fieldWithPath("isbn").type(JsonFieldType.STRING).description("Unique ISBN"),
                                fieldWithPath("totalCopies").type(JsonFieldType.NUMBER).description("Total copies owned")),
                        responseFields(
                                fieldWithPath("uuid").type(JsonFieldType.STRING).description("Book public UUID"),
                                fieldWithPath("title").type(JsonFieldType.STRING).description("Book title"),
                                fieldWithPath("author").type(JsonFieldType.STRING).description("Author name"),
                                fieldWithPath("isbn").type(JsonFieldType.STRING).description("ISBN"),
                                fieldWithPath("totalCopies").type(JsonFieldType.NUMBER).description("Total copies"),
                                fieldWithPath("availableCopies").type(JsonFieldType.NUMBER).description("Currently available copies"),
                                fieldWithPath("createdAt").type(JsonFieldType.STRING).description("Creation timestamp"),
                                fieldWithPath("updatedAt").type(JsonFieldType.STRING).description("Last update timestamp"))));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void books_list() throws Exception {
        createBook("9780132350890", 1);
        mvc.perform(get("/api/v1/books"))
                .andDo(document("books/list"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void books_get() throws Exception {
        UUID bookUuid = createBook("9780132350885", 3);
        mvc.perform(get("/api/v1/books/{uuid}", bookUuid))
                .andDo(document("books/get",
                        responseFields(
                                fieldWithPath("uuid").type(JsonFieldType.STRING).description("Book UUID"),
                                fieldWithPath("title").type(JsonFieldType.STRING).description("Title"),
                                fieldWithPath("author").type(JsonFieldType.STRING).description("Author"),
                                fieldWithPath("isbn").type(JsonFieldType.STRING).description("ISBN"),
                                fieldWithPath("totalCopies").type(JsonFieldType.NUMBER).description("Total copies"),
                                fieldWithPath("availableCopies").type(JsonFieldType.NUMBER).description("Available copies"),
                                fieldWithPath("createdAt").type(JsonFieldType.STRING).description("Created at"),
                                fieldWithPath("updatedAt").type(JsonFieldType.STRING).description("Updated at"))));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void books_update() throws Exception {
        UUID bookUuid = createBook("9780132350886", 3);
        var req = new UpdateBookRequest("New Title", null, 5);
        mvc.perform(put("/api/v1/books/{uuid}", bookUuid).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(req)))
                .andDo(document("books/update",
                        requestFields(
                                fieldWithPath("title").type(JsonFieldType.STRING).optional().description("New title (omit to keep current)"),
                                fieldWithPath("author").type(JsonFieldType.STRING).optional().description("New author"),
                                fieldWithPath("totalCopies").type(JsonFieldType.NUMBER).optional().description("New total copies"))));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void books_delete() throws Exception {
        UUID bookUuid = createBook("9780132350887", 1);
        mvc.perform(delete("/api/v1/books/{uuid}", bookUuid).with(csrf()))
                .andDo(document("books/delete"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void members_create() throws Exception {
        var req = new CreateMemberRequest("Alice", "alice" + UUID.randomUUID().toString().substring(0, 8) + "@example.com", "alice12345");
        mvc.perform(post("/api/v1/members").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(req)))
                .andDo(document("members/create",
                        requestFields(
                                fieldWithPath("name").type(JsonFieldType.STRING).description("Member full name"),
                                fieldWithPath("email").type(JsonFieldType.STRING).description("Unique email, also used as login username"),
                                fieldWithPath("password").type(JsonFieldType.STRING).description("Initial password (plaintext over HTTPS)")),
                        responseFields(
                                fieldWithPath("uuid").type(JsonFieldType.STRING).description("Member public UUID"),
                                fieldWithPath("name").type(JsonFieldType.STRING).description("Name"),
                                fieldWithPath("email").type(JsonFieldType.STRING).description("Email"),
                                fieldWithPath("createdAt").type(JsonFieldType.STRING).description("Created at"),
                                fieldWithPath("updatedAt").type(JsonFieldType.STRING).description("Updated at"))));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void members_list() throws Exception {
        createMember("list");
        mvc.perform(get("/api/v1/members"))
                .andDo(document("members/list"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void members_get() throws Exception {
        UUID memberUuid = createMember("doc-member@example.com");
        mvc.perform(get("/api/v1/members/{uuid}", memberUuid))
                .andDo(document("members/get"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void members_delete() throws Exception {
        UUID memberUuid = createMember("doc-member2@example.com");
        mvc.perform(delete("/api/v1/members/{uuid}", memberUuid).with(csrf()))
                .andDo(document("members/delete"));
    }

    @Test
    @WithMockUser(username = "member", roles = "MEMBER")
    void loans_borrow() throws Exception {
        UUID bookUuid = createBook("9780132350888", 2);
        UUID memberUuid = createMember("borrower@example.com");
        var req = new BorrowRequest(bookUuid, memberUuid);
        mvc.perform(post("/api/v1/loans").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(req)))
                .andDo(document("loans/borrow",
                        requestFields(
                                fieldWithPath("bookUuid").type(JsonFieldType.STRING).description("Book UUID"),
                                fieldWithPath("memberUuid").type(JsonFieldType.STRING).description("Member UUID")),
                        responseFields(
                                fieldWithPath("uuid").type(JsonFieldType.STRING).description("Loan UUID"),
                                fieldWithPath("bookUuid").type(JsonFieldType.STRING).description("Book UUID"),
                                fieldWithPath("memberUuid").type(JsonFieldType.STRING).description("Member UUID"),
                                fieldWithPath("borrowedAt").type(JsonFieldType.STRING).description("Borrow timestamp"),
                                fieldWithPath("dueDate").type(JsonFieldType.STRING).description("Due timestamp"),
                                fieldWithPath("returnedAt").type(JsonFieldType.STRING).optional().description("Return timestamp (null while active)"))));
    }

    @Test
    void loans_return() throws Exception {
        UUID bookUuid = createBook("9780132350889", 2);
        UUID memberUuid = createMember("returner@example.com");
        var req = new BorrowRequest(bookUuid, memberUuid);

        MvcResult borrowRes = mvc.perform(post("/api/v1/loans").with(csrf())
                        .with(user("member").roles("MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(req)))
                .andReturn();
        UUID loanUuid = UUID.fromString(om.readTree(borrowRes.getResponse().getContentAsString()).get("uuid").asText());

        mvc.perform(post("/api/v1/loans/{uuid}/return", loanUuid).with(csrf()))
                .andDo(document("loans/return"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void loans_listByMember() throws Exception {
        UUID memberUuid = createMember("listmember@example.com");
        mvc.perform(get("/api/v1/loans").param("memberId", memberUuid.toString()))
                .andDo(document("loans/list"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void loans_overdue() throws Exception {
        mvc.perform(get("/api/v1/loans/overdue"))
                .andDo(document("loans/overdue"));
    }

    private UUID createBook(String isbnPrefix, int copies) throws Exception {
        String isbn = isbnPrefix + UUID.randomUUID().toString().substring(0, 4);
        if (isbn.length() > 20) isbn = isbn.substring(0, 20);
        var req = new CreateBookRequest("The Technological Republic", "Alexander Karp", isbn, copies);
        var result = mvc.perform(post("/api/v1/books").with(csrf())
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(req)))
                .andReturn();
        String json = result.getResponse().getContentAsString();
        if (result.getResponse().getStatus() != 201) {
            throw new IllegalStateException("createBook failed: status=" + result.getResponse().getStatus() + " body=" + json);
        }
        return UUID.fromString(om.readTree(json).get("uuid").asText());
    }

    private UUID createMember(String emailPrefix) throws Exception {
        String email = UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        var req = new CreateMemberRequest("Doc User", email, "docpass123");
        var result = mvc.perform(post("/api/v1/members").with(csrf())
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(req)))
                .andReturn();
        String json = result.getResponse().getContentAsString();
        if (result.getResponse().getStatus() != 201) {
            throw new IllegalStateException("createMember failed: status=" + result.getResponse().getStatus() + " body=" + json);
        }
        return UUID.fromString(om.readTree(json).get("uuid").asText());
    }
}