package com.npx.npx;

import com.npx.npx.domain.AppUser;
import com.npx.npx.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import org.springframework.http.MediaType;

@SpringBootTest
class NpxApplicationTests {
	@Autowired
	private WebApplicationContext context;
	@Autowired
	private UserRepository users;
	private MockMvc mockMvc;

	@BeforeEach
	void setup() {
		createTestUser("User", "user@example.com", "USER");
		createTestUser("Developer", "developer@example.com", "DEVELOPER");
		createTestUser("Admin", "admin@example.com", "ADMIN");
		mockMvc = webAppContextSetup(context).apply(springSecurity()).build();
	}

	private void createTestUser(String name, String email, String role) {
		if (!users.existsByEmailIgnoreCase(email)) {
			users.save(new AppUser(name, email, "not-used-by-role-tests", role));
		}
	}

	@Test
	void contextLoads() {
	}

	@Test
	@WithMockUser(username = "user@example.com", roles = "USER")
	void userCannotAccessAdminOrDeveloperEndpoints() throws Exception {
		mockMvc.perform(get("/api/admin/users")).andExpect(status().isForbidden());
		mockMvc.perform(get("/api/developer/tasks")).andExpect(status().isForbidden());
		mockMvc.perform(get("/api/user/projects")).andExpect(status().isOk());
	}

	@Test
	@WithMockUser(username = "developer@example.com", roles = "DEVELOPER")
	void developerCannotAccessAdminOrUserEndpoints() throws Exception {
		mockMvc.perform(get("/api/admin/users")).andExpect(status().isForbidden());
		mockMvc.perform(get("/api/user/projects")).andExpect(status().isForbidden());
		mockMvc.perform(get("/api/developer/tasks")).andExpect(status().isOk());
	}

	@Test
	@WithMockUser(username = "admin@example.com", roles = "ADMIN")
	void adminCannotAccessRoleSpecificEndpoints() throws Exception {
		mockMvc.perform(get("/api/developer/tasks")).andExpect(status().isForbidden());
		mockMvc.perform(get("/api/user/projects")).andExpect(status().isForbidden());
		mockMvc.perform(get("/api/admin/users")).andExpect(status().isOk());
	}

	@Test
	void registerUserCreatesUserWithUserRole() throws Exception {
		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"New Reg\",\"email\":\"newreg@example.com\",\"password\":\"SecurePass123\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.role").value("USER"))
				.andExpect(jsonPath("$.email").value("newreg@example.com"))
				.andExpect(jsonPath("$.token").isNotEmpty());
	}

	@Test
	@WithMockUser(username = "user@example.com", roles = "USER")
	void userCanCreateProjectWithRequirementsAndDates() throws Exception {
		mockMvc.perform(post("/api/user/projects")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Portal Project\",\"description\":\"Build portal with login and dashboard\",\"startDate\":\"2026-12-01\",\"endDate\":\"2026-12-31\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Portal Project"))
				.andExpect(jsonPath("$.description").value("Build portal with login and dashboard"))
				.andExpect(jsonPath("$.startDate").value("2026-12-01"))
				.andExpect(jsonPath("$.endDate").value("2026-12-31"))
				.andExpect(jsonPath("$.createdByName").value("User"));
	}

	@Test
	@WithMockUser(username = "admin@example.com", roles = "ADMIN")
	void adminCanUpdateProjectScheduleDatesManually() throws Exception {
		String response = mockMvc.perform(post("/api/admin/projects")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Admin Scheduled Project\",\"description\":\"Requirements from client\"}"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		Number projectId = com.jayway.jsonpath.JsonPath.parse(response).read("$.id");

		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/admin/projects/" + projectId + "/schedule")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"startDate\":\"2027-01-10\",\"endDate\":\"2027-03-31\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.startDate").value("2027-01-10"))
				.andExpect(jsonPath("$.endDate").value("2027-03-31"));
	}
}
