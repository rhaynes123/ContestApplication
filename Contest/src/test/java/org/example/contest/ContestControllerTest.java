package org.example.contest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ContestControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired MutableClock clock;

    @TestConfiguration
    static class Config {
        @Bean @Primary
        MutableClock mutableClock() {
            return new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        }
    }

    static class MutableClock extends Clock {
        private final AtomicReference<Instant> now;
        MutableClock(Instant start) { this.now = new AtomicReference<>(start); }
        void set(Instant t) { now.set(t); }
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now.get(); }
    }

    private long createContest(String deadline) throws Exception {
        clock.set(Instant.parse("2026-01-01T00:00:00Z"));
        MvcResult res = mvc.perform(post("/contests")
                        .contentType("application/x-www-form-urlencoded")
                        .content("contestName=X&secretValue=500&deadline=" + deadline
                                + "&firstPrize=Gold&secondPrize=Silver&thirdPrize=Bronze"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = json.readTree(res.getResponse().getContentAsString());
        return body.get("id").asLong();
    }

    @Test
    void dtoOmitsSecretBeforeDeadline() throws Exception {
        long id = createContest("2026-02-01T00:00:00Z");
        MvcResult res = mvc.perform(get("/contests/" + id))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = json.readTree(res.getResponse().getContentAsString());
        assertThat(body.has("secretValue")).isFalse();
    }

    @Test
    void dtoIncludesSecretAfterDeadlineAndTriggersResolution() throws Exception {
        long id = createContest("2026-02-01T00:00:00Z");
        clock.set(Instant.parse("2026-01-15T00:00:00Z"));
        mvc.perform(post("/contests/" + id + "/guesses")
                        .contentType("application/x-www-form-urlencoded")
                        .content("playerName=Alice&value=499"))
                .andExpect(status().isCreated());
        clock.set(Instant.parse("2026-02-02T00:00:00Z"));
        mvc.perform(get("/contests/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secretValue").value(500))
                .andExpect(jsonPath("$.prizes[?(@.place=='FIRST')].winnerName").value("Alice"));
    }

    @Test
    void guessAfterDeadlineReturns409ContestClosed() throws Exception {
        long id = createContest("2026-02-01T00:00:00Z");
        clock.set(Instant.parse("2026-02-01T00:00:01Z"));
        mvc.perform(post("/contests/" + id + "/guesses")
                        .contentType("application/x-www-form-urlencoded")
                        .content("playerName=Alice&value=400"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("contest_closed"));
    }

    @Test
    void duplicateNameReturns409NameAlreadyGuessed() throws Exception {
        long id = createContest("2026-02-01T00:00:00Z");
        clock.set(Instant.parse("2026-01-10T00:00:00Z"));
        mvc.perform(post("/contests/" + id + "/guesses")
                        .contentType("application/x-www-form-urlencoded")
                        .content("playerName=Alice&value=400"))
                .andExpect(status().isCreated());
        mvc.perform(post("/contests/" + id + "/guesses")
                        .contentType("application/x-www-form-urlencoded")
                        .content("playerName=Alice&value=450"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("name_already_guessed"));
    }

    @Test
    void unknownContestReturns404() throws Exception {
        mvc.perform(get("/contests/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
    }
}
