package ac.ic.chaturaji.mockmvc;

import ac.ic.chaturaji.config.RootConfiguration;
import ac.ic.chaturaji.dao.UserDAO;
import ac.ic.chaturaji.web.config.WebMvcConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * @author samirarabbanian
 */

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextHierarchy({
        @ContextConfiguration(
                classes = {
                        RootConfiguration.class
                }
        ),
        @ContextConfiguration(
                classes = {
                        WebMvcConfiguration.class,
                        UserControllerMockMVCIntegrationTest.MockDaoConfiguration.class
                }
        )
})
public class UserControllerMockMVCIntegrationTest {

    @Resource
    private WebApplicationContext webApplicationContext;
    private MockMvc mockMvc;

    @Before
    public void setupFixture() {
        mockMvc = webAppContextSetup(webApplicationContext)
                .alwaysDo(print())
                .build();
    }

    @Test
    public void shouldRegisterUser() throws Exception {
        // when
        MvcResult result = mockMvc.perform(
                post("/register")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "user_one@email.com")
                        .param("password", "qazQAZ123")
                        .param("nickname", "user")
        )
                // then
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.TEXT_PLAIN + ";charset=" + StandardCharsets.UTF_8))
                .andReturn();

        assertEquals("", result.getResponse().getContentAsString());
    }

    @Test
    public void shouldValidateEmail() throws Exception {
        // when
        MvcResult result = mockMvc.perform(
                post("/register")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "user_one#email.com")
                        .param("password", "qazQAZ123")
                        .param("nickname", "user")
        )
                // then
                .andExpect(status().isBadRequest())
                .andReturn();

        assertEquals("email - Please provide a valid email", result.getResponse().getContentAsString());
    }

    @Test
    public void shouldValidatePassword() throws Exception {
        // when
        MvcResult result = mockMvc.perform(
                post("/register")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "user_one@email.com")
                        .param("password", "12")
                        .param("nickname", "user")
        )
                // then
                .andExpect(status().isBadRequest())
                .andReturn();

        assertEquals("password - Please provide a password of 8 or more characters with at least 1 digit and 1 letter", result.getResponse().getContentAsString());
    }

    @Test
    public void shouldValidateNickname() throws Exception {
        // when
        MvcResult result = mockMvc.perform(
                post("/register")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "user_one@email.com")
                        .param("password", "qazQAZ123")
                        .param("nickname", " ")
        )
                // then
                .andExpect(status().isBadRequest())
                .andReturn();

        assertEquals("nickname - Please provide a name between 3 and 50 characters", result.getResponse().getContentAsString());
    }

    @Test
    public void shouldValidateMultipleValues() throws Exception {
        // when
        MvcResult result = mockMvc.perform(
                post("/register")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "user_one@email.com")
                        .param("password", "12")
                        .param("nickname", " ")
        )
                // then
                .andExpect(status().isBadRequest())
                .andReturn();

        assertTrue(result.getResponse().getContentAsString().contains("password - Please provide a password of 8 or more characters with at least 1 digit and 1 letter"));
        assertTrue(result.getResponse().getContentAsString().contains("nickname - Please provide a name between 3 and 50 characters"));
    }

    @Configuration
    static class MockDaoConfiguration {

        @Bean
        public UserDAO userDAO() {
            return mock(UserDAO.class);
        }
    }
}
