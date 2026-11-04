package com.pensereyotdom19.dailybudget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Starts the whole server with an in-memory database and calls it like the phone would.
 * Each test registers its own user, so tests don't see each other's data.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiTest {

    @Autowired
    MockMvc mvc;

    private String register(String email) throws Exception {
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"correct horse battery\"}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    private static String unique() {
        return "u" + UUID.randomUUID() + "@example.com";
    }

    @Test
    void healthCheckIsOpen() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void apiNeedsALoginToken() throws Exception {
        mvc.perform(get("/api/bills")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/bills").header("Authorization", "Bearer not-a-real-token")).andExpect(status().isUnauthorized());
    }

    @Test
    void loginWorksWithTheRightPasswordOnly() throws Exception {
        String email = unique();
        register(email);
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email.toUpperCase() + "\",\"password\":\"correct horse battery\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"wrong password!!\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void registeringTheSameEmailTwiceIsRefused() throws Exception {
        String email = unique();
        register(email);
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"correct horse battery\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void shortPasswordsAreRefused() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + unique() + "\",\"password\":\"short\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void settingsAreSavedAndReadBack() throws Exception {
        String token = register(unique());
        mvc.perform(get("/api/settings").header("Authorization", token)).andExpect(status().isNotFound());
        mvc.perform(put("/api/settings").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"startMonth":"2026-11","monthCount":6,"totalCents":470000,
                     "livingMonthlyCents":9200,"engagementCents":150000,"firstOpenedAt":"2026-11-03T02:00:00Z"}
                    """))
            .andExpect(status().isOk());
        mvc.perform(get("/api/settings").header("Authorization", token))
            .andExpect(jsonPath("$.livingMonthlyCents").value(9200))
            .andExpect(jsonPath("$.startMonth").value("2026-11"));
    }

    @Test
    void billsCanBeSavedRepeatedlyAndDeleted() throws Exception {
        String token = register(unique());
        UUID id = UUID.randomUUID();
        String bill = """
            {"name":"Water","amountCents":500,"sortOrder":4,
             "dueMonths":["2027-02","2026-11"],"paidMonths":["2026-11"]}
            """;
        for (int i = 0; i < 2; i++) { // sending the same PUT twice must not create two bills
            mvc.perform(put("/api/bills/" + id).header("Authorization", token)
                    .contentType(MediaType.APPLICATION_JSON).content(bill))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dueMonths[0]").value("2026-11"));
        }
        mvc.perform(get("/api/bills").header("Authorization", token))
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].paidMonths[0]").value("2026-11"));

        mvc.perform(delete("/api/bills/" + id).header("Authorization", token)).andExpect(status().isNoContent());
        mvc.perform(get("/api/bills").header("Authorization", token)).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void badMonthKeysAreRefused() throws Exception {
        String token = register(unique());
        mvc.perform(put("/api/bills/" + UUID.randomUUID()).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Bank\",\"amountCents\":20000,\"sortOrder\":0,\"dueMonths\":[\"2026-13\"],\"paidMonths\":[]}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void expensesListAndMonthTotalsMatchTheAppRules() throws Exception {
        String token = register(unique());
        putExpense(token, UUID.randomUUID(), 125, "coffee", "2026-11-04T01:00:00Z", null, null);
        putExpense(token, UUID.randomUUID(), 300, "skinCare", "2026-11-10T01:00:00Z", null, null);
        putExpense(token, UUID.randomUUID(), 10000, "bills", "2026-11-04T05:00:00Z", UUID.randomUUID().toString(), "2026-11");
        putExpense(token, UUID.randomUUID(), 50000, "engagement", "2026-11-20T05:00:00Z", null, null);
        putExpense(token, UUID.randomUUID(), 999, "food", "2026-12-01T05:00:00Z", null, null); // next month

        mvc.perform(get("/api/expenses").header("Authorization", token)
                .param("from", "2026-11-01T00:00:00Z").param("to", "2026-12-01T00:00:00Z"))
            .andExpect(jsonPath("$.length()").value(4))
            .andExpect(jsonPath("$[0].category").value("engagement")); // newest first

        mvc.perform(get("/api/expenses/months/2026-11").header("Authorization", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.livingCents").value(425))      // coffee + skin care
            .andExpect(jsonPath("$.billsCents").value(10000))
            .andExpect(jsonPath("$.engagementCents").value(50000))
            .andExpect(jsonPath("$.totalCents").value(60425))
            .andExpect(jsonPath("$.byCategory.skinCare").value(300));
    }

    @Test
    void monthTotalsUseThePhonesTimeZone() throws Exception {
        String token = register(unique());
        // 20:00 UTC on Nov 30 is already Dec 1 in Phnom Penh (UTC+7).
        putExpense(token, UUID.randomUUID(), 500, "food", "2026-11-30T20:00:00Z", null, null);
        mvc.perform(get("/api/expenses/months/2026-11").header("Authorization", token).param("zone", "Asia/Phnom_Penh"))
            .andExpect(jsonPath("$.totalCents").value(0));
        mvc.perform(get("/api/expenses/months/2026-12").header("Authorization", token).param("zone", "Asia/Phnom_Penh"))
            .andExpect(jsonPath("$.totalCents").value(500));
    }

    @Test
    void usersCannotSeeOrChangeEachOthersData() throws Exception {
        String alice = register(unique());
        String bob = register(unique());
        UUID id = UUID.randomUUID();
        putExpense(alice, id, 125, "coffee", "2026-11-04T01:00:00Z", null, null);

        mvc.perform(get("/api/expenses").header("Authorization", bob)
                .param("from", "2026-11-01T00:00:00Z").param("to", "2026-12-01T00:00:00Z"))
            .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(put("/api/expenses/" + id).header("Authorization", bob).contentType(MediaType.APPLICATION_JSON)
                .content(expenseJson(1, "food", "2026-11-04T01:00:00Z", null, null)))
            .andExpect(status().isConflict());
        mvc.perform(delete("/api/expenses/" + id).header("Authorization", bob)).andExpect(status().isNotFound());
    }

    @Test
    void syncReturnsOnlyChangesIncludingDeletions() throws Exception {
        String token = register(unique());
        UUID kept = UUID.randomUUID();
        UUID removed = UUID.randomUUID();
        putExpense(token, kept, 125, "coffee", "2026-11-04T01:00:00Z", null, null);
        putExpense(token, removed, 200, "food", "2026-11-04T02:00:00Z", null, null);

        String first = mvc.perform(get("/api/sync").header("Authorization", token))
            .andExpect(jsonPath("$.expenses.length()").value(2))
            .andReturn().getResponse().getContentAsString();
        String since = JsonPath.read(first, "$.serverTime");

        Thread.sleep(5); // make sure the delete gets a later timestamp than serverTime
        mvc.perform(delete("/api/expenses/" + removed).header("Authorization", token)).andExpect(status().isNoContent());

        String second = mvc.perform(get("/api/sync").header("Authorization", token).param("since", since))
            .andExpect(jsonPath("$.expenses.length()").value(1))
            .andExpect(jsonPath("$.expenses[0].id").value(removed.toString()))
            .andExpect(jsonPath("$.expenses[0].deleted").value(true))
            .andReturn().getResponse().getContentAsString();
        assertThat((String) JsonPath.read(second, "$.serverTime")).isGreaterThan(since);
    }

    private void putExpense(String token, UUID id, long cents, String category, String at,
                            String billId, String billMonth) throws Exception {
        mvc.perform(put("/api/expenses/" + id).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content(expenseJson(cents, category, at, billId, billMonth)))
            .andExpect(status().isOk());
    }

    private static String expenseJson(long cents, String category, String at, String billId, String billMonth) {
        return "{\"amountCents\":" + cents + ",\"category\":\"" + category + "\",\"note\":\"\",\"spentAt\":\"" + at + "\""
            + (billId == null ? "" : ",\"billId\":\"" + billId + "\",\"billMonth\":\"" + billMonth + "\"") + "}";
    }
}
