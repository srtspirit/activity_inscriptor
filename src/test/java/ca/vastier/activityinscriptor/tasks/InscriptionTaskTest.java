package ca.vastier.activityinscriptor.tasks;

import ca.vastier.activityinscriptor.tasks.InscriptionTaskConstants.FliipDtoNames;
import ca.vastier.activityinscriptor.tasks.InscriptionTaskConstants.PropertyNames;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class InscriptionTaskTest {
    private static final String MAIN_URL = "mainUrl";
    private static final String LOGIN_RESOURCE = "loginUrl";
    private static final String REGISTER_CLASS_RESOURCE = "registerClassUrl";
    private static final String BOOKING_CONFIRM_RESOURCE = "bookingConfirmUrl";
    private static final String DOMAIN = "domain";
    private static final String USERNAME = "user";
    private static final String PASSWORD = "pass";
    private static final int MAX_INSCRIPTION_ATTEMPTS = 3;

    private static final String FULL_LOGIN_URL = "https://" + DOMAIN + "." + MAIN_URL + "/" + LOGIN_RESOURCE;
    private static final String FULL_CLASS_REGISTER_URL = "https://" + DOMAIN + "." + MAIN_URL + "/" + REGISTER_CLASS_RESOURCE;
    private static final String FULL_BOOKING_CONFIRM_URL = "https://" + DOMAIN + "." + MAIN_URL + "/" + BOOKING_CONFIRM_RESOURCE;
    public static final String EVENT_ID = "12345";
    public static final String DATE = "2000-01-01";
    public static final String NUMBER_OF_VISITORS = "4";

    @InjectMocks
    private InscriptionTask inscriptionTask;

    @Mock
    private RestTemplate restTemplate;
    private Map<String, Object> parameters;

    @Captor
    private ArgumentCaptor<HttpEntity<MultiValueMap<String, String>>> entityWithUrlEncodedFormCaptor;

    @BeforeEach
    public void setup()
    {
        inscriptionTask.setLoginUrl(LOGIN_RESOURCE);
        inscriptionTask.setMainUrl(MAIN_URL);
        inscriptionTask.setRegisterClassUrl(REGISTER_CLASS_RESOURCE);
        inscriptionTask.setBookingConfirmUrl(BOOKING_CONFIRM_RESOURCE);
        inscriptionTask.setMaxInscriptionAttempts(MAX_INSCRIPTION_ATTEMPTS);

        parameters = Map.of(PropertyNames.USERNAME, USERNAME,
                PropertyNames.PASSWORD, PASSWORD
                , PropertyNames.DOMAIN, DOMAIN,
                PropertyNames.EVENT_ID, EVENT_ID,
                PropertyNames.DATE, DATE,
                PropertyNames.NUMBER_OF_VISITORS, NUMBER_OF_VISITORS,
                PropertyNames.DELAY, "1");
    }

    @Test
    public void shouldInscriptForTheCourse()
    {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(createAuthResponseEntity(HttpStatus.FOUND, null))
                .thenReturn(createResponseEntity(HttpStatus.OK, null, null))
                .thenReturn(createResponseEntity(HttpStatus.OK, null, null));

        inscriptionTask.run(parameters);

        Mockito.verify(restTemplate).postForEntity(eq(FULL_LOGIN_URL), entityWithUrlEncodedFormCaptor.capture(), eq(String.class));
        var authRequestEntity = entityWithUrlEncodedFormCaptor.getValue();
        verifyAuthRequestEntity(authRequestEntity);

        Mockito.verify(restTemplate).postForEntity(eq(FULL_CLASS_REGISTER_URL), entityWithUrlEncodedFormCaptor.capture(), eq(String.class));
        var registerClassRequestEntity = entityWithUrlEncodedFormCaptor.getValue();
        verifyRegisterClassEntity(registerClassRequestEntity);

        Mockito.verify(restTemplate).postForEntity(eq(FULL_BOOKING_CONFIRM_URL), entityWithUrlEncodedFormCaptor.capture(), eq(String.class));
        var bookingConfirmRequestEntity = entityWithUrlEncodedFormCaptor.getValue();
        verifyBookingConfirmClassEntity(bookingConfirmRequestEntity);
    }

    @Test
    public void shouldRepeatInscriptionWhenItsNotStarted()
    {
        when(restTemplate.postForEntity(eq(FULL_LOGIN_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(createAuthResponseEntity(HttpStatus.FOUND, null));
        when(restTemplate.postForEntity(eq(FULL_CLASS_REGISTER_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(createResponseEntity(HttpStatus.OK, null, "plus de 2 jour(s) (48:00 heures) avant"))
                .thenReturn(createResponseEntity(HttpStatus.OK, null, null));
        when(restTemplate.postForEntity(eq(FULL_BOOKING_CONFIRM_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(createResponseEntity(HttpStatus.OK, null, null));

        inscriptionTask.run(parameters);

        Mockito.verify(restTemplate).postForEntity(eq(FULL_LOGIN_URL), entityWithUrlEncodedFormCaptor.capture(), eq(String.class));
        var authRequestEntity = entityWithUrlEncodedFormCaptor.getValue();
        verifyAuthRequestEntity(authRequestEntity);

        Mockito.verify(restTemplate, Mockito.times(2)).postForEntity(eq(FULL_CLASS_REGISTER_URL), entityWithUrlEncodedFormCaptor.capture(), eq(String.class));
        var registerClassRequestEntity = entityWithUrlEncodedFormCaptor.getValue();
        verifyRegisterClassEntity(registerClassRequestEntity);

        Mockito.verify(restTemplate).postForEntity(eq(FULL_BOOKING_CONFIRM_URL), entityWithUrlEncodedFormCaptor.capture(), eq(String.class));
        var bookingConfirmRequestEntity = entityWithUrlEncodedFormCaptor.getValue();
        verifyBookingConfirmClassEntity(bookingConfirmRequestEntity);
    }

    @Test
    public void shouldFailInscriptionWhenItsNotStartedAndAttemtsLimitIsReached()
    {
        when(restTemplate.postForEntity(eq(FULL_LOGIN_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(createAuthResponseEntity(HttpStatus.FOUND, null));
        when(restTemplate.postForEntity(eq(FULL_CLASS_REGISTER_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(createResponseEntity(HttpStatus.OK, null, "plus de 2 jour(s) (48:00 heures) avant"));

        assertThrows(RuntimeException.class, () -> inscriptionTask.run(parameters), "booking failure: plus de 2 jour(s) (48:00 heures) avant");

        Mockito.verify(restTemplate).postForEntity(eq(FULL_LOGIN_URL), entityWithUrlEncodedFormCaptor.capture(), eq(String.class));
        var authRequestEntity = entityWithUrlEncodedFormCaptor.getValue();
        verifyAuthRequestEntity(authRequestEntity);

        Mockito.verify(restTemplate, Mockito.times(MAX_INSCRIPTION_ATTEMPTS)).postForEntity(eq(FULL_CLASS_REGISTER_URL), entityWithUrlEncodedFormCaptor.capture(), eq(String.class));
        var registerClassRequestEntity = entityWithUrlEncodedFormCaptor.getValue();
        verifyRegisterClassEntity(registerClassRequestEntity);
    }

    @Test
    public void shouldFailInscriptionWhenNotAuthorized()
    {
        //given ajax login returns OK which means that login/pass incorrect as correct credentials make 302(FOUND)
        when(restTemplate.postForEntity(eq(FULL_LOGIN_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(createAuthResponseEntity(HttpStatus.OK, null));

        assertThrows(RuntimeException.class, () -> inscriptionTask.run(parameters), "login failure");

        Mockito.verify(restTemplate).postForEntity(eq(FULL_LOGIN_URL), entityWithUrlEncodedFormCaptor.capture(), eq(String.class));
        var authRequestEntity = entityWithUrlEncodedFormCaptor.getValue();
        verifyAuthRequestEntity(authRequestEntity);
    }

    @Test
    public void shouldFailInscriptionWhenBookingFailure()
    {
        when(restTemplate.postForEntity(eq(FULL_LOGIN_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(createAuthResponseEntity(HttpStatus.FOUND, null));
        when(restTemplate.postForEntity(eq(FULL_CLASS_REGISTER_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(createResponseEntity(HttpStatus.OK, null, "boom"));

        assertThrows(RuntimeException.class, () -> inscriptionTask.run(parameters), "booking failure: boom");

        Mockito.verify(restTemplate).postForEntity(eq(FULL_LOGIN_URL), entityWithUrlEncodedFormCaptor.capture(), eq(String.class));
        var authRequestEntity = entityWithUrlEncodedFormCaptor.getValue();
        verifyAuthRequestEntity(authRequestEntity);

        Mockito.verify(restTemplate).postForEntity(eq(FULL_CLASS_REGISTER_URL), entityWithUrlEncodedFormCaptor.capture(), eq(String.class));
        var registerClassRequestEntity = entityWithUrlEncodedFormCaptor.getValue();
        verifyRegisterClassEntity(registerClassRequestEntity);
    }

    @Test
    public void shouldFailInscriptionWhenConfirmationFailure()
    {
        when(restTemplate.postForEntity(eq(FULL_LOGIN_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(createAuthResponseEntity(HttpStatus.FOUND, null));
        when(restTemplate.postForEntity(eq(FULL_CLASS_REGISTER_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(createResponseEntity(HttpStatus.OK, null, null));
        when(restTemplate.postForEntity(eq(FULL_BOOKING_CONFIRM_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(createResponseEntity(HttpStatus.OK, null, "boom"));

        assertThrows(RuntimeException.class, () -> inscriptionTask.run(parameters), "confirmation failure: boom");

        Mockito.verify(restTemplate).postForEntity(eq(FULL_LOGIN_URL), entityWithUrlEncodedFormCaptor.capture(), eq(String.class));
        var authRequestEntity = entityWithUrlEncodedFormCaptor.getValue();
        verifyAuthRequestEntity(authRequestEntity);

        Mockito.verify(restTemplate).postForEntity(eq(FULL_CLASS_REGISTER_URL), entityWithUrlEncodedFormCaptor.capture(), eq(String.class));
        var registerClassRequestEntity = entityWithUrlEncodedFormCaptor.getValue();
        verifyRegisterClassEntity(registerClassRequestEntity);

        Mockito.verify(restTemplate).postForEntity(eq(FULL_BOOKING_CONFIRM_URL), entityWithUrlEncodedFormCaptor.capture(), eq(String.class));
        var bookingConfirmRequestEntity = entityWithUrlEncodedFormCaptor.getValue();
        verifyBookingConfirmClassEntity(bookingConfirmRequestEntity);
    }

    private ResponseEntity<String> createAuthResponseEntity(final HttpStatus httpStatus, final String errorMessage)
    {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.LOCATION, "/home/dashboard");
        httpHeaders.add("set-cookie", "ci-session=vngreigvirnggvirt;phpid=bvingbg");

        return createResponseEntity(httpStatus, httpHeaders, errorMessage);
    }

    private ResponseEntity<String> createResponseEntity(final HttpStatus httpStatus, final HttpHeaders httpHeaders, final String errorMessage)
    {
        final String body;
        if (errorMessage != null && !errorMessage.isEmpty())
        {
            body = "{\"error\": true, \"message\": \"" + errorMessage + "\"}";
        }
        else
        {
            body = "{\"error\": false}";
        }
        return ResponseEntity.status(httpStatus).headers(httpHeaders).body(body);
    }

    private void verifyAuthRequestEntity(final HttpEntity<MultiValueMap<String, String>> httpEntity)
    {
        var body = httpEntity.getBody();

        assertEquals(USERNAME, body.getFirst(FliipDtoNames.USERNAME));
        assertEquals(PASSWORD, body.getFirst(FliipDtoNames.PASSWORD));
    }

    private void verifyRegisterClassEntity(final HttpEntity<MultiValueMap<String, String>> httpEntity)
    {
        var body = httpEntity.getBody();
        final HttpHeaders httpHeaders = httpEntity.getHeaders();
        assertEquals(MediaType.APPLICATION_FORM_URLENCODED, httpHeaders.getContentType());
        assertEquals("XMLHttpRequest", httpHeaders.getFirst("x-requested-with"));
        assertNotNull(httpHeaders.get("cookie"));

        assertEquals(EVENT_ID, body.getFirst(FliipDtoNames.EVENT_ID));
        assertEquals(DATE, body.getFirst(FliipDtoNames.DATE));
    }
    private void verifyBookingConfirmClassEntity(final HttpEntity<MultiValueMap<String, String>> httpEntity)
    {
        var body = httpEntity.getBody();
        final HttpHeaders httpHeaders = httpEntity.getHeaders();
        assertEquals(MediaType.APPLICATION_FORM_URLENCODED, httpHeaders.getContentType());
        assertEquals("XMLHttpRequest", httpHeaders.getFirst("x-requested-with"));
        assertNotNull(httpHeaders.get("cookie")); //TODO assert on cookie

        assertEquals(EVENT_ID, body.getFirst(FliipDtoNames.EVENT_ID));
        assertEquals(DATE, body.getFirst(FliipDtoNames.DATE));
        assertEquals("207239", body.getFirst(FliipDtoNames.CONTRACT_ID));
        assertEquals(NUMBER_OF_VISITORS, body.getFirst(FliipDtoNames.NUMBER_OF_VISITORS));
    }
}
