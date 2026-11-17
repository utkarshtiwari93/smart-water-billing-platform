package com.infosys.Water_Usage_Monitoring.controller;

import com.jayway.jsonpath.JsonPath;
import com.infosys.Water_Usage_Monitoring.entity.Household;
import com.infosys.Water_Usage_Monitoring.entity.AuthChallenge;
import com.infosys.Water_Usage_Monitoring.entity.AuthChallenge.Purpose;
import com.infosys.Water_Usage_Monitoring.entity.BillingCycle;
import com.infosys.Water_Usage_Monitoring.entity.Invoice;
import com.infosys.Water_Usage_Monitoring.entity.InvoiceItem;
import com.infosys.Water_Usage_Monitoring.entity.TariffPlan;
import com.infosys.Water_Usage_Monitoring.entity.User;
import com.infosys.Water_Usage_Monitoring.entity.WaterUsageLog;
import com.infosys.Water_Usage_Monitoring.repository.ApartmentRepository;
import com.infosys.Water_Usage_Monitoring.repository.AuthChallengeRepository;
import com.infosys.Water_Usage_Monitoring.repository.BillingCycleRepository;
import com.infosys.Water_Usage_Monitoring.repository.HouseholdRepository;
import com.infosys.Water_Usage_Monitoring.repository.InvoiceRepository;
import com.infosys.Water_Usage_Monitoring.repository.TariffPlanRepository;
import com.infosys.Water_Usage_Monitoring.repository.UserRepository;
import com.infosys.Water_Usage_Monitoring.repository.WaterUsageLogRepository;
import com.infosys.Water_Usage_Monitoring.enums.CycleStatus;
import com.infosys.Water_Usage_Monitoring.enums.InvoiceItemType;
import com.infosys.Water_Usage_Monitoring.enums.ReadingSource;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    @Autowired ApartmentRepository apartmentRepository;
    @Autowired BillingCycleRepository billingCycleRepository;
    @Autowired HouseholdRepository householdRepository;
    @Autowired InvoiceRepository invoiceRepository;
    @Autowired WaterUsageLogRepository usageRepository;
    @Autowired TariffPlanRepository tariffPlanRepository;
    @Autowired UserRepository userRepository;
    @Autowired AuthChallengeRepository authChallengeRepository;
    @Autowired PasswordEncoder passwordEncoder;
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
        long apartmentId = idOf(r);
        User admin = userRepository.findByEmail(adminEmail).orElseThrow();
        admin.setApartment(apartmentRepository.findById(apartmentId).orElseThrow());
        userRepository.save(admin);
        return apartmentId;
    }

    private long createUnassignedApartment(String token) throws Exception {
        MvcResult r = send(post("/api/v1/apartments"), token,
                "{\"name\":\"Other Apt\",\"address\":\"Addr\",\"city\":\"Noida\"}")
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
        createRegistrationChallenge(email, "654321");
        MvcResult r = send(post("/api/v1/auth/register"), null,
                "{\"name\":\"Ravi\",\"email\":\"" + email + "\",\"password\":\"Ravi@1234\",\"apartmentId\":"
                        + apartmentId + hh + ",\"verificationCode\":\"654321\"}")
                .andExpect(status().isCreated()).andReturn();
        return idOf(r);
    }

    private void createRegistrationChallenge(String email, String code) {
        createAuthChallenge(email, Purpose.REGISTRATION, code);
    }

    private void createAuthChallenge(String email, Purpose purpose, String code) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        AuthChallenge challenge = authChallengeRepository
                .findForUpdate(email.toLowerCase(java.util.Locale.ROOT), purpose)
                .orElseGet(AuthChallenge::new);
        challenge.setEmail(email.toLowerCase(java.util.Locale.ROOT));
        challenge.setPurpose(purpose);
        challenge.setCodeHash(passwordEncoder.encode(code));
        challenge.setResetTokenHash(null);
        challenge.setAttempts(0);
        challenge.setExpiresAt(now.plusMinutes(10));
        challenge.setSentAt(now.minusMinutes(1));
        authChallengeRepository.save(challenge);
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
                .andExpect(status().isBadRequest());
        org.junit.jupiter.api.Assertions.assertFalse(userRepository.existsByEmail(email));

        createRegistrationChallenge(email, "123456");
        send(post("/api/v1/auth/register"), null,
                "{\"name\":\"Ravi\",\"email\":\"" + email + "\",\"password\":\"Ravi@1234\",\"apartmentId\":" + apt
                        + ",\"verificationCode\":\"000000\"}")
                .andExpect(status().isBadRequest());
        org.junit.jupiter.api.Assertions.assertFalse(userRepository.existsByEmail(email));

        createRegistrationChallenge(email, "123456");
        send(post("/api/v1/auth/register"), null,
                "{\"name\":\"Ravi\",\"email\":\"" + email + "\",\"password\":\"Ravi@1234\",\"apartmentId\":" + apt
                        + ",\"verificationCode\":\"123456\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("RESIDENT"));

        send(post("/api/v1/auth/register"), null,
                "{\"name\":\"Ravi\",\"email\":\"" + email + "\",\"password\":\"Ravi@1234\",\"apartmentId\":" + apt
                        + ",\"verificationCode\":\"123456\"}")
                .andExpect(status().isConflict());

        createAuthChallenge(email, Purpose.PASSWORD_RESET, "987654");
        String resetResponse = send(post("/api/v1/auth/password/verify-code"), null,
                "{\"email\":\"" + email + "\",\"code\":\"987654\"}")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String resetToken = JsonPath.read(resetResponse, "$.resetToken");
        send(post("/api/v1/auth/password/reset"), null,
                "{\"email\":\"" + email + "\",\"resetToken\":\"" + resetToken
                        + "\",\"newPassword\":\"NewRavi@1234\"}")
                .andExpect(status().isOk());
        send(post("/api/v1/auth/login"), null,
                "{\"email\":\"" + email + "\",\"password\":\"Ravi@1234\"}")
                .andExpect(status().isUnauthorized());
        login(email, "NewRavi@1234");

        send(post("/api/v1/auth/register"), null,
                "{\"name\":\"Ravi\",\"email\":\"" + uniqueEmail() + "\",\"password\":\"123\",\"apartmentId\":" + apt + "}")
                .andExpect(status().isBadRequest());

        // Public auth responses stay privacy-safe even when SMTP is not configured in tests.
        send(post("/api/v1/auth/register/send-code"), null, "{\"email\":\"" + email + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(
                        "If this email can be registered, a verification code has been sent."));
        send(post("/api/v1/auth/password/forgot"), null, "{\"email\":\"nobody-" + email + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(
                        "If an account exists for this email, a password reset code has been sent."));
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

    @Test
    void admin_cannotReadOrWriteAnotherApartmentsData() throws Exception {
        String admin = adminToken();
        long assignedApartment = createApartment(admin);
        long otherApartment = createUnassignedApartment(admin);
        String otherMeter = uniqueMeter();
        Household household = new Household();
        household.setApartment(apartmentRepository.findById(otherApartment).orElseThrow());
        household.setFlatNumber("X-101");
        household.setAreaSqft(new java.math.BigDecimal("900"));
        household.setOccupancy(2);
        household.setMeterNumber(otherMeter);
        long householdId = householdRepository.save(household).getId();
        TariffPlan tariff = new TariffPlan();
        tariff.setApartment(household.getApartment());
        tariff.setName("Other apartment tariff");
        tariff.setTier1LimitKl(new BigDecimal("10.00"));
        tariff.setBaseRate(new BigDecimal("1.00"));
        tariff.setHigherRate(new BigDecimal("2.00"));
        tariff.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        TariffPlan savedTariff = tariffPlanRepository.save(tariff);
        BillingCycle cycle = new BillingCycle();
        cycle.setApartment(household.getApartment());
        cycle.setTariffPlan(savedTariff);
        cycle.setStartDate(LocalDate.of(2026, 11, 1));
        cycle.setEndDate(LocalDate.of(2026, 11, 30));
        cycle.setStatus(CycleStatus.OPEN);
        BillingCycle savedCycle = billingCycleRepository.save(cycle);
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber("S-" + UUID.randomUUID());
        invoice.setBillingCycle(savedCycle);
        invoice.setHousehold(household);
        invoice.setConsumptionLitres(BigDecimal.ZERO);
        invoice.setTotalAmount(BigDecimal.ZERO);
        Long invoiceId = invoiceRepository.save(invoice).getId();

        send(get("/api/v1/apartments/" + otherApartment), admin, null).andExpect(status().isForbidden());
        send(get("/api/v1/apartments/" + otherApartment + "/households"), admin, null)
                .andExpect(status().isForbidden());
        send(get("/api/v1/households/" + householdId), admin, null).andExpect(status().isForbidden());
        send(get("/api/v1/usage?householdId=" + householdId), admin, null).andExpect(status().isForbidden());
        postUsage(admin, householdId, "2026-11-01", "100").andExpect(status().isForbidden());
        send(get("/api/v1/users?apartmentId=" + otherApartment), admin, null)
                .andExpect(status().isForbidden());
        send(get("/api/v1/invoices/" + invoiceId), admin, null).andExpect(status().isForbidden());
        send(get("/api/v1/invoices/" + invoiceId + "/pdf"), admin, null).andExpect(status().isForbidden());
        send(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                        "/api/v1/tariffs/" + savedTariff.getId() + "/activation?active=false"), admin, null)
                .andExpect(status().isForbidden());
        MockMultipartFile csv = new MockMultipartFile(
                "file", "readings.csv", "text/csv",
                ("meterNumber,readingDate,meterReading\n" + otherMeter + ",2026-11-02,100\n")
                        .getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/api/v1/usage/upload").file(csv).header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(0))
                .andExpect(jsonPath("$.failed").value(1));

        assertEquals(assignedApartment,
                userRepository.findByEmail(adminEmail).orElseThrow().getApartment().getId());
    }

    @Test
    void invoicePdf_downloadRendersSavedChargesAndMeterDetailsAndRestrictsResidents() throws Exception {
        String admin = adminToken();
        long apartmentId = createApartment(admin);
        long householdId = createHousehold(admin, apartmentId, "A-101", uniqueMeter());
        long otherHouseholdId = createHousehold(admin, apartmentId, "A-102", uniqueMeter());
        String residentEmail = uniqueEmail();
        registerResident(apartmentId, householdId, residentEmail);
        String resident = login(residentEmail, "Ravi@1234");
        String otherResidentEmail = uniqueEmail();
        registerResident(apartmentId, otherHouseholdId, otherResidentEmail);
        String otherResident = login(otherResidentEmail, "Ravi@1234");

        Household household = householdRepository.findById(householdId).orElseThrow();
        TariffPlan tariff = new TariffPlan();
        tariff.setApartment(household.getApartment());
        tariff.setName("PDF integration tariff");
        tariff.setTier1LimitKl(new BigDecimal("10.00"));
        tariff.setBaseRate(new BigDecimal("10.00"));
        tariff.setHigherRate(new BigDecimal("20.00"));
        tariff.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        tariff = tariffPlanRepository.save(tariff);
        BillingCycle cycle = new BillingCycle();
        cycle.setApartment(household.getApartment());
        cycle.setTariffPlan(tariff);
        cycle.setStartDate(LocalDate.of(2026, 10, 1));
        cycle.setEndDate(LocalDate.of(2026, 10, 31));
        cycle.setStatus(CycleStatus.FINALIZED);
        cycle = billingCycleRepository.save(cycle);

        saveReading(household, "2026-09-30", "100000");
        saveReading(household, "2026-10-31", "112000");

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber("PDF-" + UUID.randomUUID().toString().substring(0, 8));
        invoice.setBillingCycle(cycle);
        invoice.setHousehold(household);
        invoice.setConsumptionLitres(new BigDecimal("12000.00"));
        invoice.setTotalAmount(new BigDecimal("450.00"));
        invoice.addItem(invoiceItem(InvoiceItemType.WATER_TIER1, "Water charge - tier 1", "100.00"));
        invoice.addItem(invoiceItem(InvoiceItemType.WATER_TIER2, "Water charge - tier 2", "200.00"));
        invoice.addItem(invoiceItem(InvoiceItemType.SHARED_AREA, "Shared-area allocation", "100.00"));
        invoice.addItem(invoiceItem(InvoiceItemType.ADJUSTMENT, "Approved adjustment", "50.00"));
        invoice = invoiceRepository.saveAndFlush(invoice);

        byte[] pdfBytes = send(get("/api/v1/invoices/" + invoice.getId() + "/pdf"), resident, null)
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_PDF))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andReturn().getResponse().getContentAsByteArray();
        try (PDDocument pdf = Loader.loadPDF(pdfBytes)) {
            String text = new PDFTextStripper().getText(pdf);
            assertTrue(text.contains("Test Apt"));
            assertTrue(text.contains("A-101"));
            assertTrue(text.contains("100000 litres on 30 Sep 2026"));
            assertTrue(text.contains("112000 litres on 31 Oct 2026"));
            assertTrue(text.contains("12000.00 litres"));
            assertTrue(text.contains("Shared-area allocation"));
            assertTrue(text.contains("Approved adjustment"));
            assertTrue(text.contains("INR 450.00"));
        }

        send(get("/api/v1/invoices/" + invoice.getId() + "/pdf"), otherResident, null)
                .andExpect(status().isForbidden());
        send(get("/api/v1/invoices/" + invoice.getId() + "/pdf"), admin, null)
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_PDF));
    }

    private void saveReading(Household household, String date, String meter) {
        WaterUsageLog reading = new WaterUsageLog();
        reading.setHousehold(household);
        reading.setReadingDate(LocalDate.parse(date));
        reading.setMeterReading(new BigDecimal(meter));
        reading.setConsumptionLitres(BigDecimal.ZERO);
        reading.setSource(ReadingSource.MANUAL);
        usageRepository.save(reading);
    }

    private InvoiceItem invoiceItem(InvoiceItemType type, String description, String amount) {
        InvoiceItem item = new InvoiceItem();
        item.setItemType(type);
        item.setDescription(description);
        item.setAmount(new BigDecimal(amount));
        return item;
    }

    // ---------- apartment CRUD ----------
    @Test
    void apartmentCrud_createReadUpdate_andRestrictDeleteOutsideScope() throws Exception {
        String admin = adminToken();
        long originalApartmentId = userRepository.findByEmail(adminEmail).orElseThrow()
                .getApartment().getId();
        long apt = createApartment(admin);

        send(get("/api/v1/apartments/" + apt), admin, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Test Apt"));

        send(put("/api/v1/apartments/" + apt), admin,
                "{\"name\":\"Renamed\",\"address\":\"New Addr\",\"city\":\"Lucknow\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Renamed"));

        User adminUser = userRepository.findByEmail(adminEmail).orElseThrow();
        adminUser.setApartment(apartmentRepository.findById(originalApartmentId).orElseThrow());
        userRepository.save(adminUser);
        send(delete("/api/v1/apartments/" + apt), admin, null).andExpect(status().isForbidden());
        send(get("/api/v1/apartments/" + apt), admin, null).andExpect(status().isForbidden());
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
        long otherApt = createUnassignedApartment(admin);
        Household otherHousehold = new Household();
        otherHousehold.setApartment(apartmentRepository.findById(otherApt).orElseThrow());
        otherHousehold.setFlatNumber("D-1");
        otherHousehold.setAreaSqft(new java.math.BigDecimal("900"));
        otherHousehold.setOccupancy(2);
        otherHousehold.setMeterNumber(uniqueMeter());
        long otherHh = householdRepository.save(otherHousehold).getId();
        send(put("/api/v1/users/" + resident + "/household"), admin, "{\"householdId\":" + otherHh + "}")
                .andExpect(status().isForbidden());

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

        send(get("/api/v1/usage?householdId=" + hh + "&from=2026-10-01&to=2026-10-02"), admin, null)
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