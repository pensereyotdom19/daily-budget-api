package com.pensereyotdom19.dailybudget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.hamcrest.Matchers;
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
    void dashboardFilesAreServedWithoutATokenAndWithSecurityHeaders() throws Exception {
        mvc.perform(get("/index.html"))
            .andExpect(status().isOk())
            .andExpect(content().string(Matchers.containsString("<script src=\"/app.js\"")))
            .andExpect(header().string("Content-Security-Policy", Matchers.allOf(
                Matchers.containsString("script-src 'self'"),
                Matchers.containsString("style-src 'self'"),
                Matchers.containsString("connect-src 'self'"),
                Matchers.containsString("frame-ancestors 'none'"),
                Matchers.not(Matchers.containsString("unsafe-inline")))))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(header().string("X-Frame-Options", "DENY"))
            .andExpect(header().string("Referrer-Policy", "no-referrer"));
        mvc.perform(get("/app.js")).andExpect(status().isOk());
        mvc.perform(get("/app.css")).andExpect(status().isOk());
        mvc.perform(get("/favicon.svg")).andExpect(status().isOk());
        // "/" shows the same page (Spring forwards it to index.html).
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(forwardedUrl("index.html"));
    }

    @Test
    void openingTheDashboardDoesNotOpenTheApi() throws Exception {
        for (String path : new String[] {"/api/sync", "/api/settings", "/api/bills", "/api/expenses/months/2026-11",
                                         "/api/expenses?from=2026-11-01T00:00:00Z&to=2026-12-01T00:00:00Z"}) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
        mvc.perform(get("/some-other-file.txt")).andExpect(status().isUnauthorized());
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

    @Test
    void incomesAndRecurringExpensesSyncIncludingDeletions() throws Exception {
        String token = register(unique());
        UUID gift = UUID.randomUUID();
        UUID topUp = UUID.randomUUID();
        mvc.perform(put("/api/incomes/" + gift).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"amountCents":2000,"target":"savings","note":" Gift ","receivedAt":"2026-11-04T03:00:00Z"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.note").value("Gift"));
        mvc.perform(put("/api/recurring/" + topUp).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"amountCents":500,"category":"phone","note":"Top-up","dayOfMonth":5,
                     "startMonth":"2026-11","lastAddedMonth":"2026-11","active":true}
                    """))
            .andExpect(status().isOk());

        String first = mvc.perform(get("/api/sync").header("Authorization", token))
            .andExpect(jsonPath("$.incomes.length()").value(1))
            .andExpect(jsonPath("$.incomes[0].target").value("savings"))
            .andExpect(jsonPath("$.recurring.length()").value(1))
            .andExpect(jsonPath("$.recurring[0].lastAddedMonth").value("2026-11"))
            .andExpect(jsonPath("$.recurring[0].category").value("phone"))
            .andReturn().getResponse().getContentAsString();
        String since = JsonPath.read(first, "$.serverTime");

        Thread.sleep(5);
        mvc.perform(delete("/api/incomes/" + gift).header("Authorization", token)).andExpect(status().isNoContent());
        mvc.perform(get("/api/sync").header("Authorization", token).param("since", since))
            .andExpect(jsonPath("$.incomes.length()").value(1))
            .andExpect(jsonPath("$.incomes[0].deleted").value(true))
            .andExpect(jsonPath("$.recurring.length()").value(0));
    }

    @Test
    void badIncomeTargetsAndDaysAreRefusedAndOthersCannotSeeThem() throws Exception {
        String token = register(unique());
        String other = register(unique());
        UUID id = UUID.randomUUID();
        mvc.perform(put("/api/incomes/" + id).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"amountCents\":100,\"target\":\"casino\",\"note\":\"\",\"receivedAt\":\"2026-11-04T03:00:00Z\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(put("/api/recurring/" + id).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"amountCents\":100,\"category\":\"food\",\"note\":\"\",\"dayOfMonth\":32,\"startMonth\":\"2026-11\",\"active\":true}"))
            .andExpect(status().isBadRequest());
        mvc.perform(put("/api/incomes/" + id).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"amountCents\":100,\"target\":\"living\",\"note\":\"\",\"receivedAt\":\"2026-11-04T03:00:00Z\"}"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/incomes").header("Authorization", other)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(delete("/api/incomes/" + id).header("Authorization", other)).andExpect(status().isNotFound());
        mvc.perform(put("/api/incomes/" + id).header("Authorization", other).contentType(MediaType.APPLICATION_JSON)
                .content("{\"amountCents\":1,\"target\":\"living\",\"note\":\"\",\"receivedAt\":\"2026-11-04T03:00:00Z\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void planHistoryIsSavedAndKeptWhenAnOlderAppLeavesItOut() throws Exception {
        String token = register(unique());
        mvc.perform(put("/api/settings").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"startMonth":"2027-05","monthCount":6,"totalCents":470000,"livingMonthlyCents":9200,
                     "engagementCents":150000,"historyStartMonth":"2026-11","savedBeforeCents":12345}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.savedBeforeCents").value(12345));
        mvc.perform(put("/api/settings").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"startMonth":"2027-05","monthCount":6,"totalCents":480000,"livingMonthlyCents":9200,"engagementCents":150000}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.historyStartMonth").value("2026-11"))
            .andExpect(jsonPath("$.savedBeforeCents").value(12345));
    }

    @Test
    void fiveWrongPasswordsLockTheAccountForAWhile() throws Exception {
        String email = unique();
        register(email);
        String wrong = "{\"email\":\"" + email + "\",\"password\":\"wrong password!!\"}";
        String right = "{\"email\":\"" + email + "\",\"password\":\"correct horse battery\"}";
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(wrong)
                    .with(r -> { r.setRemoteAddr("10.9.0." + 1); return r; }))
                .andExpect(status().isUnauthorized());
        }
        // Even the right password waits now, from any address.
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(right)
                .with(r -> { r.setRemoteAddr("10.9.0.2"); return r; }))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().exists("Retry-After"))
            .andExpect(jsonPath("$.detail").value(Matchers.startsWith("Too many attempts")));
    }

    @Test
    void unknownEmailsGetTheSameAnswerAsWrongPasswords() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + unique() + "\",\"password\":\"whatever12345\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.detail").value("Wrong email or password"));
    }

    @Test
    void hugeRequestsAreRefusedBeforeTheyAreRead() throws Exception {
        String big = "{\"email\":\"a@b.co\",\"password\":\"" + "x".repeat(70_000) + "\"}";
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(big))
            .andExpect(status().isPayloadTooLarge());
    }

    @Test
    void httpsRepliesTellBrowsersToStayOnHttps() throws Exception {
        mvc.perform(get("/").secure(true))
            .andExpect(header().string("Strict-Transport-Security", Matchers.containsString("max-age=31536000")))
            .andExpect(header().string("Permissions-Policy", Matchers.containsString("camera=()")));
    }

    @Test
    void tokensFromAnotherIssuerAreRefused() throws Exception {
        // Signed with the right key but claiming another issuer.
        var key = new javax.crypto.spec.SecretKeySpec(
            "test-secret-0123456789abcdef0123456789abcdef".getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
        var encoder = new org.springframework.security.oauth2.jwt.NimbusJwtEncoder(
            new com.nimbusds.jose.jwk.source.ImmutableSecret<>(key));
        var now = java.time.Instant.now();
        var claims = org.springframework.security.oauth2.jwt.JwtClaimsSet.builder()
            .issuer("someone-else").subject(UUID.randomUUID().toString())
            .issuedAt(now).expiresAt(now.plusSeconds(600)).build();
        var header = org.springframework.security.oauth2.jwt.JwsHeader.with(
            org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build();
        String token = encoder.encode(org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(header, claims))
            .getTokenValue();
        mvc.perform(get("/api/bills").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }

    // ---------- sign out everywhere ----------

    private String login(String email) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"correct horse battery\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    /** A token signed with the test key, like TokenService makes, but with any "ver" (null = no claim). */
    private static String handMadeToken(UUID userId, Integer ver) {
        var key = new javax.crypto.spec.SecretKeySpec(
            "test-secret-0123456789abcdef0123456789abcdef".getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
        var encoder = new org.springframework.security.oauth2.jwt.NimbusJwtEncoder(
            new com.nimbusds.jose.jwk.source.ImmutableSecret<>(key));
        var now = java.time.Instant.now();
        var claims = org.springframework.security.oauth2.jwt.JwtClaimsSet.builder()
            .issuer("daily-budget-api").subject(userId.toString())
            .issuedAt(now).expiresAt(now.plusSeconds(600));
        if (ver != null) claims.claim("ver", ver);
        var header = org.springframework.security.oauth2.jwt.JwsHeader.with(
            org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build();
        return "Bearer " + encoder.encode(org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(header, claims.build()))
            .getTokenValue();
    }

    private static UUID userIdOf(String bearer) {
        String payload = bearer.substring("Bearer ".length()).split("\\.")[1];
        String json = new String(java.util.Base64.getUrlDecoder().decode(payload), java.nio.charset.StandardCharsets.UTF_8);
        return UUID.fromString(JsonPath.read(json, "$.sub"));
    }

    @Test
    void signOutEverywhereEndsEveryOldTokenAndANewLoginWorks() throws Exception {
        String email = unique();
        String phone = register(email);
        String laptop = login(email);
        mvc.perform(get("/api/sync").header("Authorization", phone)).andExpect(status().isOk());
        mvc.perform(get("/api/sync").header("Authorization", laptop)).andExpect(status().isOk());

        mvc.perform(post("/api/auth/logout-all").header("Authorization", laptop)).andExpect(status().isNoContent());

        // Both old tokens stop working at once, also the one that asked.
        mvc.perform(get("/api/sync").header("Authorization", phone)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/sync").header("Authorization", laptop)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/logout-all").header("Authorization", laptop)).andExpect(status().isUnauthorized());

        String fresh = login(email);
        mvc.perform(get("/api/sync").header("Authorization", fresh)).andExpect(status().isOk());
    }

    @Test
    void tokensWithTheCurrentVersionKeepWorking() throws Exception {
        String email = unique();
        String first = register(email);
        String second = login(email);
        // Many requests in a row (the version is remembered for a while) keep working.
        for (int i = 0; i < 3; i++) {
            mvc.perform(get("/api/sync").header("Authorization", first)).andExpect(status().isOk());
            mvc.perform(get("/api/bills").header("Authorization", second)).andExpect(status().isOk());
        }
        // Tokens made before "ver" existed count as version 0, which is every user's starting version.
        UUID userId = userIdOf(first);
        mvc.perform(get("/api/sync").header("Authorization", handMadeToken(userId, null))).andExpect(status().isOk());
        mvc.perform(get("/api/sync").header("Authorization", handMadeToken(userId, 0))).andExpect(status().isOk());
        mvc.perform(get("/api/sync").header("Authorization", handMadeToken(userId, 1))).andExpect(status().isUnauthorized());

        mvc.perform(post("/api/auth/logout-all").header("Authorization", first)).andExpect(status().isNoContent());
        mvc.perform(get("/api/sync").header("Authorization", handMadeToken(userId, null))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/sync").header("Authorization", handMadeToken(userId, 1))).andExpect(status().isOk());
    }

    @Test
    void tokensOfUnknownUsersAreRefused() throws Exception {
        mvc.perform(get("/api/sync").header("Authorization", handMadeToken(UUID.randomUUID(), 0)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void signOutEverywhereNeedsAToken() throws Exception {
        mvc.perform(post("/api/auth/logout-all")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/logout-all").header("Authorization", "Bearer not-a-real-token"))
            .andExpect(status().isUnauthorized());
        // Login and register are still open.
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + unique() + "\",\"password\":\"whatever12345\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.detail").value("Wrong email or password"));
    }

    // ---------- category limits ----------

    private static String plan(String extra) {
        return "{\"startMonth\":\"2026-11\",\"monthCount\":6,\"totalCents\":470000,"
            + "\"livingMonthlyCents\":9200,\"engagementCents\":150000" + (extra == null ? "" : "," + extra) + "}";
    }

    private org.springframework.test.web.servlet.ResultActions putPlan(String token, String extra) throws Exception {
        return mvc.perform(put("/api/settings").header("Authorization", token)
            .contentType(MediaType.APPLICATION_JSON).content(plan(extra)));
    }

    @Test
    void categoryLimitsAreSavedKeptWhenLeftOutAndClearedByAnEmptyObject() throws Exception {
        String token = register(unique());
        putPlan(token, null).andExpect(status().isOk()).andExpect(jsonPath("$.categoryLimits").isMap())
            .andExpect(jsonPath("$.categoryLimits.length()").value(0));

        putPlan(token, "\"categoryLimits\":{\"coffee\":1500,\"food\":3000,\"skinCare\":0}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.categoryLimits.coffee").value(1500));
        mvc.perform(get("/api/settings").header("Authorization", token))
            .andExpect(jsonPath("$.categoryLimits.coffee").value(1500))
            .andExpect(jsonPath("$.categoryLimits.food").value(3000))
            .andExpect(jsonPath("$.categoryLimits.skinCare").value(0));

        // An older app leaves the field out (or sends null): the limits stay.
        putPlan(token, null).andExpect(status().isOk()).andExpect(jsonPath("$.categoryLimits.coffee").value(1500));
        putPlan(token, "\"categoryLimits\":null").andExpect(status().isOk())
            .andExpect(jsonPath("$.categoryLimits.food").value(3000));

        // {} clears them.
        putPlan(token, "\"categoryLimits\":{}").andExpect(status().isOk())
            .andExpect(jsonPath("$.categoryLimits").isMap())
            .andExpect(jsonPath("$.categoryLimits.length()").value(0));
        mvc.perform(get("/api/settings").header("Authorization", token))
            .andExpect(jsonPath("$.categoryLimits.length()").value(0));
    }

    @Test
    void badCategoryLimitsAreRefused() throws Exception {
        String token = register(unique());
        putPlan(token, "\"categoryLimits\":{\"bills\":100}").andExpect(status().isBadRequest());
        putPlan(token, "\"categoryLimits\":{\"engagement\":100}").andExpect(status().isBadRequest());
        putPlan(token, "\"categoryLimits\":{\"casino\":100}").andExpect(status().isBadRequest());
        putPlan(token, "\"categoryLimits\":{\"coffee\":-1}").andExpect(status().isBadRequest());
        putPlan(token, "\"categoryLimits\":{\"coffee\":100000001}").andExpect(status().isBadRequest());
        putPlan(token, "\"categoryLimits\":{\"coffee\":null}").andExpect(status().isBadRequest());
        putPlan(token, "\"categoryLimits\":[1500]").andExpect(status().isBadRequest());
        putPlan(token, "\"categoryLimits\":{\"coffee\":100000000}").andExpect(status().isOk());

        // More than 20 entries. Only 10 keys are allowed, so JSON with repeated keys is the only way to send
        // 21; check the size rule directly on the request record instead.
        var many = new java.util.HashMap<String, Long>();
        for (int i = 0; i < 21; i++) many.put("k" + i, 1L);
        var request = new com.pensereyotdom19.dailybudget.settings.SettingsDtos.SettingsRequest(
            "2026-11", 6, 470000, 9200, 150000, null, null, null, many);
        var violations = jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator().validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("categoryLimits")
            && v.getConstraintDescriptor().getAnnotation() instanceof jakarta.validation.constraints.Size);
    }

    @Test
    void syncIncludesCategoryLimits() throws Exception {
        String token = register(unique());
        putPlan(token, null).andExpect(status().isOk());
        mvc.perform(get("/api/sync").header("Authorization", token))
            .andExpect(jsonPath("$.settings.categoryLimits").isMap())
            .andExpect(jsonPath("$.settings.categoryLimits.length()").value(0));
        putPlan(token, "\"categoryLimits\":{\"fun\":2500}").andExpect(status().isOk());
        mvc.perform(get("/api/sync").header("Authorization", token))
            .andExpect(jsonPath("$.settings.categoryLimits.fun").value(2500));
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
