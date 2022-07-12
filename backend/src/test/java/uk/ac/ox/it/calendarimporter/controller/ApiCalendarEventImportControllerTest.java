package uk.ac.ox.it.calendarimporter.controller;

import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.ac.ox.it.calendarimporter.persistence.repo.TenantRepository;
import uk.ac.ox.it.calendarimporter.security.WithMockClaims;
import uk.ac.ox.it.calendarimporter.service.JobSchedulingService;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApiCalendarEventImportController.class)
public class ApiCalendarEventImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TenantRepository tenantRepository;

    @MockBean
    private JobSchedulingService jobSchedulingService;

    private Number userId1 = 456;

    private String calendarUrl1 = "https://blavatnik-calendar-import/user/456.csv";

    @Test
    @WithMockClaims(claims = "{'https://purl.imsglobal.org/spec/lti/claim/custom': {'canvas_user_id': 456, 'url': 'https://blavatnik-calendar-import/user/456.csv'} }")
    public void testSubscribeToCalendarImport() throws Exception {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("date_last_ran", new Date());
        jsonObject.put("isUserSubscribed", true);
        Mockito.when(jobSchedulingService.subscribeUserToCalendarImport( userId1, calendarUrl1)).thenReturn(jsonObject);

        MvcResult result =  mockMvc.perform(MockMvcRequestBuilders.post("/api/subscribe"))
                .andExpect(status().isOk()).andReturn();

        JSONObject jsObject = (JSONObject) new JSONParser(JSONParser.MODE_PERMISSIVE).parse(result.getResponse().getContentAsString());
        assertEquals(new SimpleDateFormat("yyyyMMdd").format(new Date()), new SimpleDateFormat("yyyyMMdd").format(new Date(Instant.parse((CharSequence) jsObject.get("date_last_ran")).toEpochMilli())));
        assertEquals(true, jsObject.get("isUserSubscribed"));
    }

    @Test
    @WithMockClaims(claims = "{'https://purl.imsglobal.org/spec/lti/claim/custom': {'canvas_user_id': 456, 'url': 'https://blavatnik-calendar-import/user/456.csv'} }")
    public void testUnSubscribeFromCalendarImport() throws Exception {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("date_last_ran", null);
        jsonObject.put("isUserSubscribed", false);
        Mockito.when(jobSchedulingService.unsubscribeUserFromCalendarImport( userId1, calendarUrl1)).thenReturn(jsonObject);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/unsubscribe"))
                .andExpect(status().isOk()).andReturn();

        JSONObject jsObject = (JSONObject) new JSONParser(JSONParser.MODE_PERMISSIVE).parse(result.getResponse().getContentAsString());
        assertEquals(null, jsObject.get("date_last_ran"));
        assertEquals(false, jsObject.get("isUserSubscribed"));
    }

    @Test
    @WithMockClaims(claims = "{'https://purl.imsglobal.org/spec/lti/claim/custom': {'canvas_user_id': 456, 'url': 'https://blavatnik-calendar-import/user/456.csv'} }")
    public void isUserSubscribed() throws Exception {
        Mockito.when(jobSchedulingService.isUserSubscribed( userId1, calendarUrl1)).thenReturn(true);

        MvcResult result =  mockMvc.perform(MockMvcRequestBuilders.get("/api/isUserSubscribed"))
                .andExpect(status().isOk()).andReturn();

        assertEquals(true, new JSONParser(JSONParser.MODE_PERMISSIVE).parse(result.getResponse().getContentAsString()));
    }
}
