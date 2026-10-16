package com.infosys.Water_Usage_Monitoring.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ApiIntegrationTest {

    @Autowired WebApplicationContext context;
    @Value("${app.admin.email}") String adminEmail;
    @Value("${app.admin.password}") String adminPassword;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    // ---------- helpers ----------
    private String bearer(String token) {
        return "Bearer " + token;
    }

    private ResultActions send(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder b,
                               String token, String body) throws Exception {
        if (token != null) b.header("Authorization", bearer(token));
        if (body != null) b.contentType(MediaType.APPLICATION_JSON).content(body);
        return mvc.perform(b);
    }

    private long idOf(MvcResult r) throws Exception {
        Number n = JsonPath.read(r.getResponse().getContentAsString(), "$.id");
        return n.longValue();
    }

    private String login(String email, String password) throws Exception {
        String body = send(post("/api/v1/auth/login"), null,
                "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private String adminToken() throws Exception {
        return login(adminEmail, adminPassword);
    }

    private long createApartment(String token) throws Exception {
        MvcResult r = send(post("/api/v1/apartments"), token,
                "{\"name\":\"Test Apt\",\"address\":\"Addr\",\"city\":\"Noida\"}")
                .andExpect(status().isCreated()).andReturn();
        return idOf(r);
    }

    private long createHousehold(String token, long apartmentId, String flat, String meter) throws Exception {
        MvcResult r = send(post("/api/v1/apartments/" + apartmentId + "/households"), token,
                "{\"flatNumber\":\"" + flat + "\",\"areaSqft\":1100,\"occupancy\":3,\"meterNumber\":\"" + meter + "\"}")
                .andExpect(status().isCreated()).andReturn();
        return idOf(r);
    }

    private String uniqueEmail() {
        return "res-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
    }

    private String uniqueMeter() {
        return "MTR-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private long registerResident(long apartmentId, Long householdId, String email) throws Exception {
        String hh = (householdId == null) ? "" : ",\"householdId\":" + householdId;
        MvcResult r = send(post("/api/v1/auth/register"), null,
                "{\"name\":\"Ravi\",\"email\":\"" + email + "\",\"password\":\"Ravi@1234\",\"apartmentId\":"
                        + apartmentId + hh + "}")
                .andExpect(status().isCreated()).andReturn();
        return idOf(r);
    }

    private ResultActions postUsage(String token, long householdId, String date, String reading) throws Exception {
        return send(post("/api/v1/usage"), token,
                "{\"householdId\":" + householdId + ",\"readingDate\":\"" + date + "\",\"meterReading\":" + reading + "}");
    }

    private double consumptionOf(ResultActions a) throws Exception {
        String body = a.andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.consumptionLitres")).doubleValue();
    }

    // ---------- auth & security ----------
    @Test
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mvc.perform(get("/api/v1/apartments")).andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withGarbageToken_returns401() throws Exception {
        send(get("/api/v1/apartments"), "not.a.token", null).andExpect(status().isUnauthorized());
    }

    @Test
    void login_asSeededAdmin_returnsTokenAndAdminRole() throws Exception {
        send(post("/api/v1/auth/login"), null,
                "{\"email\":\"" + adminEmail + "\",\"password\":\"" + adminPassword + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        send(post("/api/v1/auth/login"), null,
                "{\"email\":\"" + adminEmail + "\",\"password\":\"definitely-wrong\"}")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void register_createsResident_andRejectsDuplicateEmailAndWeakPassword() throws Exception {
        String admin = adminToken();
        long apt = createApartment(admin);
        String email = uniqueEmail();

        send(post("/api/v1/auth/register"), null,
                "{\"name\":\"Ravi\",\"email\":\"" + email + "\",\"password\":\"Ravi@1234\",\"apartmentId\":" + apt + "}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("RESIDENT"));

        send(post("/api/v1/auth/register"), null,
                "{\"name\":\"Ravi\",\"email\":\"" + email + "\",\"password\":\"Ravi@1234\",\"apartmentId\":" + apt + "}")
                .andExpect(status().isConflict());

        send(post("/api/v1/auth/register"), null,
                "{\"name\":\"Ravi\",\"email\":\"" + uniqueEmail() + "\",\"password\":\"123\",\"apartmentId\":" + apt + "}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void resident_cannotCallAdminEndpoints_returns403() throws Exception {
        String admin = adminToken();
        long apt = createApartment(admin);
        String email = uniqueEmail();
        registerResident(apt, null, email);
        String resident = login(email, "Ravi@1234");

        send(post("/api/v1/apartments"), resident,
                "{\"name\":\"Hacked\",\"address\":\"x\",\"city\":\"y\"}")
                .andExpect(status().isForbidden());
        send(get("/api/v1/users"), resident, null).andExpect(status().isForbidden());
    }

    // ---------- apartment CRUD ----------
    @Test
    void apartmentCrud_createReadUpdateDelete() throws Exception {
        String admin = adminToken();
        long apt = createApartment(admin);

        send(get("/api/v1/apartments/" + apt), admin, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Test Apt"));

        send(put("/api/v1/apartments/" + apt), admin,
                "{\"name\":\"Renamed\",\"address\":\"New Addr\",\"city\":\"Lucknow\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Renamed"));

        send(delete("/api/v1/apartments/" + apt), admin, null).andExpect(status().isNoContent());
        send(get("/api/v1/apartments/" + apt), admin, null).andExpect(status().isNotFound());
    }

    @Test
    void createApartment_blankName_returns400WithFieldErrors() throws Exception {
        send(post("/api/v1/apartments"), adminToken(), "{\"name\":\"\",\"city\":\"Noida\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void getUnknownApartment_returns404() throws Exception {
        send(get("/api/v1/apartments/999999"), adminToken(), null).andExpect(status().isNotFound());
    }

    // ---------- household CRUD ----------
    @Test
    void householdCrud_createListUpdateDelete_andDuplicateRules() throws Exception {
        String admin = adminToken();
        long apt = createApartment(admin);
        String meter = uniqueMeter();
        long hh = createHousehold(admin, apt, "B-202", meter);

        send(post("/api/v1/apartments/" + apt + "/households"), admin,
                "{\"flatNumber\":\"B-202\",\"areaSqft\":900,\"occupancy\":2}")
                .andExpect(status().isConflict());

        send(post("/api/v1/apartments/" + apt + "/households"), admin,
                "{\"flatNumber\":\"B-203\",\"areaSqft\":900,\"occupancy\":2,\"meterNumber\":\"" + meter + "\"}")
                .andExpect(status().isConflict());

        send(post("/api/v1/apartments/" + apt + "/households"), admin,
                "{\"flatNumber\":\"\",\"areaSqft\":900,\"occupancy\":2}")
                .andExpect(status().isBadRequest());

        send(get("/api/v1/apartments/" + apt + "/households"), admin, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));

        send(put("/api/v1/households/" + hh), admin,
                "{\"flatNumber\":\"B-202\",\"areaSqft\":1500,\"occupancy\":5,\"meterNumber\":\"" + meter + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.occupancy").value(5));

        send(delete("/api/v1/households/" + hh), admin, null).andExpect(status().isNoContent());
        send(get("/api/v1/households/" + hh), admin, null).andExpect(status().isNotFound());
    }

    // ---------- resident assignment & profile ----------
    @Test
    void admin_assignsAndUnassignsResident_withRules() throws Exception {
        String admin = adminToken();
        long apt = createApartment(admin);
        long hh = createHousehold(admin, apt, "C-301", uniqueMeter());
        long resident = registerResident(apt, null, uniqueEmail());

        send(put("/api/v1/users/" + resident + "/household"), admin, "{\"householdId\":" + hh + "}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.householdId").value(hh));

        // household doosre apartment ka -> 400
        long otherApt = createApartment(admin);
        long otherHh = createHousehold(admin, otherApt, "D-1", uniqueMeter());
        send(put("/api/v1/users/" + resident + "/household"), admin, "{\"householdId\":" + otherHh + "}")
                .andExpect(status().isBadRequest());

        send(delete("/api/v1/users/" + resident + "/household"), admin, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.householdId").doesNotExist());
    }

    @Test
    void resident_updatesProfileAndChangesPassword() throws Exception {
        String admin = adminToken();
        long apt = createApartment(admin);
        String email = uniqueEmail();
        registerResident(apt, null, email);
        String token = login(email, "Ravi@1234");

        send(get("/api/v1/users/me"), token, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(email));

        send(put("/api/v1/users/me"), token, "{\"name\":\"Ravi Kumar\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Ravi Kumar"));

        send(put("/api/v1/users/me/password"), token,
                "{\"currentPassword\":\"wrong-one\",\"newPassword\":\"Naya@1234\"}")
                .andExpect(status().isBadRequest());

        send(put("/api/v1/users/me/password"), token,
                "{\"currentPassword\":\"Ravi@1234\",\"newPassword\":\"Naya@1234\"}")
                .andExpect(status().isNoContent());

        login(email, "Naya@1234");   // naye password se login chalna chahiye
        send(post("/api/v1/auth/login"), null,
                "{\"email\":\"" + email + "\",\"password\":\"Ravi@1234\"}")
                .andExpect(status().isUnauthorized());
    }

    // ---------- usage logging ----------
    @Test
    void usageFlow_baselineThenDifference_andRejections() throws Exception {
        String admin = adminToken();
        long apt = createApartment(admin);
        long hh = createHousehold(admin, apt, "A-101", uniqueMeter());

        ResultActions first = postUsage(admin, hh, "2026-10-01", "1250").andExpect(status().isCreated());
        assertEquals(0.0, consumptionOf(first), 0.001);

        ResultActions second = postUsage(admin, hh, "2026-10-02", "1262").andExpect(status().isCreated());
        assertEquals(12.0, consumptionOf(second), 0.001);

        postUsage(admin, hh, "2026-10-02", "1270").andExpect(status().isConflict());        // duplicate date
        postUsage(admin, hh, "2026-10-03", "1200").andExpect(status().isBadRequest());      // reading ghati
        postUsage(admin, hh, "2026-09-30", "1300").andExpect(status().isBadRequest());      // purani date
        postUsage(admin, hh, "2099-01-01", "1300").andExpect(status().isBadRequest());      // future date

        send(get("/api/v1/usage?householdId=" + hh), admin, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void resident_seesOwnUsage_butNotAnotherHouseholds() throws Exception {
        String admin = adminToken();
        long apt = createApartment(admin);
        long own = createHousehold(admin, apt, "A-1", uniqueMeter());
        long other = createHousehold(admin, apt, "A-2", uniqueMeter());
        String email = uniqueEmail();
        registerResident(apt, own, email);
        String resident = login(email, "Ravi@1234");

        send(get("/api/v1/usage?householdId=" + own), resident, null).andExpect(status().isOk());
        send(get("/api/v1/usage?householdId=" + other), resident, null).andExpect(status().isForbidden());
        postUsage(resident, own, "2026-10-01", "100").andExpect(status().isForbidden());
    }

    @Test
    void csvUpload_importsValidRows_andReportsBadOnes() throws Exception {
        String admin = adminToken();
        long apt = createApartment(admin);
        String meter = uniqueMeter();
        createHousehold(admin, apt, "A-101", meter);

        String content = "meterNumber,readingDate,meterReading\n"
                + meter + ",2026-10-01,1000\n"
                + meter + ",2026-10-02,1015\n"
                + meter + ",2026-10-02,1020\n"
                + "NO-SUCH-METER,2026-10-01,5\n";
        MockMultipartFile file = new MockMultipartFile("file", "usage.csv", "text/csv",
                content.getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart("/api/v1/usage/upload").file(file).header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(4))
                .andExpect(jsonPath("$.imported").value(2))
                .andExpect(jsonPath("$.failed").value(2));
    }

    @Test
    void csvUpload_asResident_returns403() throws Exception {
        String admin = adminToken();
        long apt = createApartment(admin);
        String email = uniqueEmail();
        registerResident(apt, null, email);
        String resident = login(email, "Ravi@1234");

        MockMultipartFile file = new MockMultipartFile("file", "usage.csv", "text/csv",
                "meterNumber,readingDate,meterReading\n".getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart("/api/v1/usage/upload").file(file).header("Authorization", bearer(resident)))
                .andExpect(status().isForbidden());
    }
}