package hu.ogyh.voting.controller;

import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hu.ogyh.voting.service.VotingService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

@DisplayName("API: a specifikáció végpontjai, JSON-formái és hibakódjai a teljes alkalmazáson keresztül")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VotingApiTest {

    private static final String BASE = "/szavazasok";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    /** Csak a nehezen előidézhető váratlan hibához; minden más teszt a valódi viselkedést használja. */
    @MockitoSpyBean
    private VotingService votingService;

    @Nested
    @DisplayName("1. Szavazás mentése")
    class Save {

        @Test
        // az azonosító formáját a VotingIdGeneratorTest ellenőrzi
        @DisplayName("1. A spec példakérése 200-at és szavazásazonosítót ad")
        void savesExampleRequest() throws Exception {
            save(template())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.szavazasId").isString());
        }

        @Test
        @DisplayName("1. Hibás mezők: Validációs hiba, mezőnkénti lista a JSON-nevekkel")
        void validationErrorsUseJsonNames() throws Exception {
            ObjectNode request = template();
            request.put("targy", "");
            vote(request, 0).put("kepviselo", "Kepviselo 1");

            save(request)
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.title").value("Validációs hiba"))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.instance").value(BASE + "/szavazas"))
                    .andExpect(jsonPath("$.hibak.length()").value(2))
                    .andExpect(jsonPath("$.hibak[?(@.mezo == 'targy')].uzenet").value("nem lehet üres"))
                    .andExpect(jsonPath("$.hibak[?(@.mezo == 'szavazatok[0].kepviselo')].uzenet")
                            .value("csak betűt, számot és aláhúzást tartalmazhat"));
        }

        @Test
        @DisplayName("1. Ismeretlen kód: Olvashatatlan kérés a hibás mezővel és kóddal")
        void unknownCode() throws Exception {
            ObjectNode request = template();
            vote(request, 1).put("szavazat", "x");

            save(request)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Olvashatatlan kérés"))
                    .andExpect(jsonPath("$.detail").value("Ismeretlen kód a(z) szavazatok[1].szavazat mezőben: x"));
        }

        @Test
        @DisplayName("1. Nem ISO-8601 időpont: Olvashatatlan kérés a hibás mezővel és értékkel")
        void invalidVotedTime() throws Exception {
            ObjectNode request = template();
            request.put("idopont", "tegnap");

            save(request)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Olvashatatlan kérés"))
                    .andExpect(jsonPath("$.detail").value("Érvénytelen érték a(z) idopont mezőben: tegnap"));
        }

        @Test
        @DisplayName("1. Nem értelmezhető JSON: Olvashatatlan kérés")
        void malformedJson() throws Exception {
            mockMvc.perform(post(BASE + "/szavazas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"targy\": "))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Olvashatatlan kérés"))
                    .andExpect(jsonPath("$.detail").value("A kérés törzse nem értelmezhető JSON"));
        }

        @Test
        @DisplayName("1. Üzleti szabály sérül: Érvénytelen kérés a hiba okával")
        void businessRuleViolation() throws Exception {
            ObjectNode request = template();
            vote(request, 2).put("kepviselo", "Kepviselo2");

            save(request)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Érvénytelen kérés"))
                    .andExpect(jsonPath("$.detail").value("Több szavazatot adott le: Kepviselo2"));
        }
    }

    @Nested
    @DisplayName("2–3. Képviselő szavazata és szavazás eredménye")
    class VoteAndResult {

        @Test
        @DisplayName("2. A képviselő szavazata kóddal; nem létező szavazásra és nem szavazó képviselőre eltérő 404")
        void memberVote() throws Exception {
            String publicId = savedId(template());

            mockMvc.perform(get(BASE + "/szavazat").param("szavazas", publicId).param("kepviselo", "Kepviselo2"))
                    .andExpect(status().isOk())
                    .andExpect(content().json("{\"szavazat\": \"n\"}", JsonCompareMode.STRICT));
            mockMvc.perform(get(BASE + "/szavazat").param("szavazas", publicId).param("kepviselo", "Kepviselo9"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Nem található"))
                    .andExpect(jsonPath("$.detail")
                            .value("A(z) Kepviselo9 képviselő nem szavazott a(z) " + publicId + " szavazáson"));
            mockMvc.perform(get(BASE + "/szavazat").param("szavazas", "XX0000").param("kepviselo", "Kepviselo1"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value("Nem található szavazás ezzel az azonosítóval: XX0000"));
        }

        @Test
        @DisplayName("3. Az eredmény a spec szerinti mezőkkel; nem létező szavazásra 404")
        void result() throws Exception {
            String publicId = savedId(template());

            mockMvc.perform(get(BASE + "/eredmeny").param("szavazas", publicId))
                    .andExpect(status().isOk())
                    .andExpect(content()
                            .json(
                                    """
                                    {"eredmeny": "F", "kepviselokSzama": 3, "igenekSzama": 1,
                                     "nemekSzama": 1, "tartozkodasokSzama": 1}""",
                                    JsonCompareMode.STRICT));
            mockMvc.perform(get(BASE + "/eredmeny").param("szavazas", "XX0000")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("4. Napi szavazások")
    class Daily {

        @Test
        @DisplayName("4. Időrendben, teljes adatokkal, eredménnyel és szavazatokkal")
        void dailyVotings() throws Exception {
            // fordított sorrendben mentve: a válasz időrendje így a lekérdezés rendezését bizonyítja
            ObjectNode qualified = template();
            qualified.put("idopont", "2023-12-13T14:31:00Z");
            qualified.put("tipus", "m");
            save(qualified).andExpect(status().isOk());
            ObjectNode presence = template();
            presence.put("idopont", "2023-12-13T14:30:00.750Z");
            save(presence).andExpect(status().isOk());

            mockMvc.perform(get(BASE + "/napi-szavazasok").param("nap", "2023-12-13"))
                    .andExpect(status().isOk())
                    .andExpect(content().json(resource("daily-votings-response.json"), JsonCompareMode.STRICT));
        }

        @Test
        @DisplayName("4. Üres napon üres lista")
        void emptyDay() throws Exception {
            mockMvc.perform(get(BASE + "/napi-szavazasok").param("nap", "2023-12-13"))
                    .andExpect(status().isOk())
                    .andExpect(content().json("{\"szavazasok\": []}", JsonCompareMode.STRICT));
        }
    }

    @Nested
    @DisplayName("5. Kimutatások")
    class Statistics {

        @Test
        @DisplayName("5.1 A részvételi átlag 2 tizedesre, a spec szerinti formában")
        void participationAverage() throws Exception {
            saveAt("2023-12-10T10:00:00Z", "e", "n", "Kepviselo1", "Kepviselo2");
            saveAt("2023-12-11T10:00:00Z", "e", "n", "Kepviselo1");

            mockMvc.perform(get(BASE + "/kepviselo-reszvetel-atlag")
                            .param("tol", "2023-12-01")
                            .param("ig", "2023-12-31"))
                    .andExpect(status().isOk())
                    .andExpect(content().json("{\"atlag\": 1.50}", JsonCompareMode.STRICT));
        }

        @Test
        @DisplayName("5.2 A spec példájával azonos válasz")
        void specialProcedures() throws Exception {
            saveAt("2023-12-10T10:00:00Z", "j", "s", "Kepviselo1");
            saveAt("2023-12-10T10:01:00Z", "j", "s", "Kepviselo1");
            saveAt("2023-12-10T10:02:00Z", "j", "k", "Kepviselo1");
            // egyetlen jelenlévő nemmel szavaz: elutasított
            ObjectNode rejected = template();
            rejected.put("idopont", "2023-12-10T10:03:00Z");
            rejected.put("tipus", "e");
            rejected.put("eljaras", "s");
            rejected.putArray("szavazatok")
                    .addObject()
                    .put("kepviselo", "Kepviselo1")
                    .put("szavazat", "n");
            save(rejected).andExpect(status().isOk());

            mockMvc.perform(get(BASE + "/kulonleges-eljarasok-szama")
                            .param("tol", "2023-12-01")
                            .param("ig", "2023-12-31"))
                    .andExpect(status().isOk())
                    .andExpect(content().json(resource("special-procedures-response.json"), JsonCompareMode.STRICT));
        }

        private void saveAt(String time, String type, String procedure, String... yesVoters) throws Exception {
            ObjectNode request = template();
            request.put("idopont", time);
            request.put("tipus", type);
            request.put("eljaras", procedure);
            var votes = request.putArray("szavazatok");
            for (String voter : yesVoters) {
                votes.addObject().put("kepviselo", voter).put("szavazat", "i");
            }
            save(request).andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("2.4 Hibakezelés: paraméterhibák, váratlan hiba és a keretrendszer hibái magyarul")
    class FrameworkErrors {

        @Test
        @DisplayName("2.4 Hiányzó paraméter")
        void missingParameter() throws Exception {
            mockMvc.perform(get(BASE + "/eredmeny"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Hiányzó paraméter"))
                    .andExpect(jsonPath("$.detail").value("Hiányzik a(z) szavazas paraméter"));
        }

        @Test
        @DisplayName("2.4 Érvénytelen paraméter")
        void invalidParameter() throws Exception {
            mockMvc.perform(get(BASE + "/napi-szavazasok").param("nap", "tegnap"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Érvénytelen paraméter"))
                    .andExpect(jsonPath("$.detail").value("Érvénytelen érték a(z) nap paraméterben: tegnap"));
        }

        @Test
        @DisplayName("2.4 Váratlan hiba: Belső hiba, belső részletek nélkül")
        void unexpectedErrorHidesInternals() throws Exception {
            doThrow(new IllegalStateException("belső részlet: jdbc:h2:mem:voting"))
                    .when(votingService)
                    .getResult("XX9999");

            mockMvc.perform(get(BASE + "/eredmeny").param("szavazas", "XX9999"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(content()
                            .json(
                                    """
                                    {"title": "Belső hiba", "status": 500, "detail": "Váratlan hiba történt",
                                     "instance": "/szavazasok/eredmeny"}""",
                                    JsonCompareMode.STRICT));
        }

        @Test
        @DisplayName("2.4 Nem létező útvonal, nem támogatott metódus és tartalomtípus")
        void notFoundMethodAndMediaType() throws Exception {
            mockMvc.perform(get(BASE + "/nincs-ilyen"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Nem található"));
            mockMvc.perform(put(BASE + "/szavazas"))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(header().exists("Allow"))
                    .andExpect(jsonPath("$.title").value("Nem támogatott metódus"));
            mockMvc.perform(post(BASE + "/szavazas")
                            .contentType(MediaType.TEXT_PLAIN)
                            .content("x"))
                    .andExpect(status().isUnsupportedMediaType())
                    .andExpect(jsonPath("$.title").value("Nem támogatott tartalomtípus"));
        }
    }

    private ResultActions save(ObjectNode request) throws Exception {
        return mockMvc.perform(post(BASE + "/szavazas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));
    }

    private String savedId(ObjectNode request) throws Exception {
        String body = save(request)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return jsonMapper.readTree(body).get("szavazasId").asString();
    }

    private static ObjectNode vote(ObjectNode request, int index) {
        return (ObjectNode) request.withArray("szavazatok").get(index);
    }

    private ObjectNode template() throws IOException {
        return (ObjectNode) jsonMapper.readTree(resource("save-voting-request.json"));
    }

    private static String resource(String name) throws IOException {
        return new ClassPathResource("api/" + name).getContentAsString(StandardCharsets.UTF_8);
    }
}
