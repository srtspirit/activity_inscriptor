package ca.vastier.activityinscriptor.tasks;

import ca.vastier.activityinscriptor.tasks.InscriptionTaskConstants.FliipDtoNames;
import ca.vastier.activityinscriptor.tasks.InscriptionTaskConstants.PropertyNames;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;


@Component("ACTIVITY_INSCRIPTOR") //TODO find a way to name beans
public class InscriptionTask implements Task
{
	private final static Logger LOGGER = LoggerFactory.getLogger(InscriptionTask.class);
	private final static String ERROR_FIELD = "error";
	private final static String MESSAGE_FIELD = "message";
	private final static long DEFAULT_POLL_INSCRIPTION_OPENING_INTERVAL_MS = 950L;

	//TODO decouple spring's rest template
	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;

	//TODO inject properties in a better way
	@Setter
	@Value("${task.inscriptor.url}")
	private String mainUrl;
	@Setter
	@Value("${task.inscriptor.login-url}")
	private String loginUrl;
	@Setter
	@Value("${task.inscriptor.booking-url}")
	private String registerClassUrl;
	@Setter
	@Value("${task.inscriptor.confirm-booking-url}")
	private String bookingConfirmUrl;
	@Setter
	@Value("${task.inscriptor.max-attempts}")
	private int maxInscriptionAttempts;

	@Autowired
	public InscriptionTask(final RestTemplate restTemplate)
	{
		this.restTemplate = restTemplate;
		this.objectMapper = new ObjectMapper();
	}

	public void run(final Map<String, Object> parameters)
	{
		final ResponseEntity<String> authenticationResponse = authenticate(parameters);

		LOGGER.info("sent authentification request... Responded with status code: {}",
				authenticationResponse.getStatusCodeValue());
		LOGGER.debug("The response body: {}", authenticationResponse.getBody());

		assertSuccessfulLogin(authenticationResponse);
		final Collection<String> cookiesToPass = extractCookiesWithValuesFromHeaders(authenticationResponse.getHeaders());

		ResponseEntity<String> bookingResponse = null;

		final String givenDelay = (String) parameters.get(PropertyNames.DELAY);
		final long delayBetweenTries = givenDelay != null ? Long.parseLong(givenDelay) : DEFAULT_POLL_INSCRIPTION_OPENING_INTERVAL_MS;

		for (int i = 0; i < maxInscriptionAttempts; i++)
		{
			bookingResponse = bookPlaceInEvent(parameters, cookiesToPass);
			LOGGER.info("sent booking request... Responded with status code: {}",
					bookingResponse.getStatusCodeValue(), bookingResponse.getBody());
			LOGGER.debug("The response body: {}", bookingResponse.getBody());

			if (inscriptionIsNotYetOpen(bookingResponse))
			{
				LOGGER.info("The inscription has not started yet");
				try
				{
					Thread.sleep(delayBetweenTries);
				}
				catch (InterruptedException e)
				{
				}
			}
			else
			{
				break;
			}
		}

		assertSuccessfulBooking(bookingResponse);

		final ResponseEntity<String> confirmationResponse = confirmBooking(parameters, cookiesToPass);
		LOGGER.info("sent booking confirmation... Responded with status code: {}",
				confirmationResponse.getStatusCodeValue());
		LOGGER.debug("The response body: {}", confirmationResponse.getBody());
		assertSuccessfulConfirmation(confirmationResponse);
	}

	private ResponseEntity<String> confirmBooking(final Map<String, Object> parameters, final Collection<String> cookiesToPass)
	{
		final StringBuilder uriSb = new StringBuilder();
		uriSb.append("https://");
		uriSb.append((String) parameters.get(PropertyNames.DOMAIN));
		uriSb.append(".");
		uriSb.append(mainUrl);
		uriSb.append("/");
		uriSb.append(bookingConfirmUrl);

		final HttpHeaders confirmBookingHeaders = new HttpHeaders();
		confirmBookingHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		confirmBookingHeaders.add("x-requested-with", "XMLHttpRequest");

		cookiesToPass.forEach(cookie -> confirmBookingHeaders.add("cookie", cookie));

		final MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add(FliipDtoNames.EVENT_ID, (String) parameters.get(PropertyNames.EVENT_ID));
		map.add(FliipDtoNames.DATE, (String) parameters.get(PropertyNames.DATE));
		map.add(FliipDtoNames.CONTRACT_ID, findContractNumber());
		map.add(FliipDtoNames.NUMBER_OF_VISITORS, (String) parameters.get(PropertyNames.NUMBER_OF_VISITORS)); //not mandatory. null is 1

		final HttpEntity<MultiValueMap<String, String>> bookingRequest = new HttpEntity<>(map, confirmBookingHeaders);

		return restTemplate.postForEntity(uriSb.toString(), bookingRequest, String.class);
	}

	private ResponseEntity<String> bookPlaceInEvent(final Map<String, Object> parameters, final Collection<String> cookiesToPass)
	{
		final StringBuilder uriSb = new StringBuilder();
		uriSb.append("https://");
		uriSb.append((String) parameters.get(PropertyNames.DOMAIN));
		uriSb.append(".");
		uriSb.append(mainUrl);
		uriSb.append("/");
		uriSb.append(registerClassUrl);

		final HttpHeaders bookingHeaders = new HttpHeaders();
		bookingHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		bookingHeaders.add("x-requested-with", "XMLHttpRequest");

		cookiesToPass.forEach(cookie -> bookingHeaders.add("cookie", cookie));

		final MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add(FliipDtoNames.EVENT_ID, (String) parameters.get(PropertyNames.EVENT_ID));
		map.add(FliipDtoNames.DATE, (String) parameters.get(PropertyNames.DATE));
		map.add(FliipDtoNames.NUMBER_OF_VISITORS, (String)parameters.get(PropertyNames.NUMBER_OF_VISITORS)); //not mandatory. null is 1

		final HttpEntity<MultiValueMap<String, String>> bookingRequest = new HttpEntity<>(map, bookingHeaders);

		return restTemplate.postForEntity(uriSb.toString(), bookingRequest, String.class);
	}

	private ResponseEntity<String> authenticate(final Map<String, Object> parameters)
	{
		final StringBuilder uriSb = new StringBuilder();
		uriSb.append("https://");
		uriSb.append((String) parameters.get(PropertyNames.DOMAIN));
		uriSb.append(".");
		uriSb.append(mainUrl);
		uriSb.append("/");
		uriSb.append(loginUrl);

		final HttpHeaders loginHeaders = new HttpHeaders();
		loginHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		final MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add(FliipDtoNames.USERNAME, (String) parameters.get(PropertyNames.USERNAME));
		map.add(FliipDtoNames.PASSWORD, (String) parameters.get(PropertyNames.PASSWORD));

		final HttpEntity<MultiValueMap<String, String>> loginRequest = new HttpEntity<>(map, loginHeaders);

		return restTemplate.postForEntity(uriSb.toString(), loginRequest, String.class);
	}

	private void assertSuccessfulLogin(final ResponseEntity<String> responseEntity)
	{
		if (!HttpStatus.FOUND.equals(responseEntity.getStatusCode()) || !responseEntity.getHeaders()
				.get(HttpHeaders.LOCATION)
				.stream()
				.anyMatch(str -> str.contains("/home/dashboard")))
		{
			//TODO create good exception and pass error description
			throw new RuntimeException("login failure");
		}
	}

	private void assertSuccessfulBooking(final ResponseEntity<String> responseEntity)
	{
		ObjectNode objectNode = null;
		try
		{
			objectNode = objectMapper.readValue(extractJsonString(responseEntity.getBody()), ObjectNode.class);
		}
		catch (JsonProcessingException e)
		{
			//TODO create a good exception
			throw new RuntimeException("booking failure");
		}

		if (!HttpStatus.OK.equals(responseEntity.getStatusCode()) || objectNode.get(ERROR_FIELD).booleanValue())
		{
			//TODO create a good exception
			throw new RuntimeException(String.format("booking failure: %s", objectNode.get(MESSAGE_FIELD)));
		}
	}

	private void assertSuccessfulConfirmation(final ResponseEntity<String> responseEntity)
	{
		ObjectNode objectNode = null;
		try
		{
			objectNode = objectMapper.readValue(extractJsonString(responseEntity.getBody()), ObjectNode.class);
		}
		catch (JsonProcessingException e)
		{
			//TODO create a good exception
			throw new RuntimeException("confirmation failure");
		}

		if (!HttpStatus.OK.equals(responseEntity.getStatusCode()) || objectNode.get(ERROR_FIELD).booleanValue())
		{
			//TODO create a good exception
			throw new RuntimeException(String.format("confirmation failure: %s", objectNode.get(MESSAGE_FIELD)));
		}
	}

	private boolean inscriptionIsNotYetOpen(final ResponseEntity<String> responseEntity)
	{
		if (HttpStatus.OK.equals(responseEntity.getStatusCode()))
		{
			final ObjectMapper objectMapper = new ObjectMapper();
			ObjectNode objectNode;
			try
			{
				objectNode = objectMapper.readValue(extractJsonString(responseEntity.getBody()), ObjectNode.class);
			}
			catch (JsonProcessingException e)
			{
				return false;
			}

			return objectNode.get(ERROR_FIELD).booleanValue() && objectNode.get(MESSAGE_FIELD)
					.asText()
					.contains("plus de 2 jour(s) (48:00 heures) avant");
		}

		return false;

	}

	private Collection<String> extractCookiesWithValuesFromHeaders(final HttpHeaders headers, final String... searchCookieNames)
	{
		return headers.get("set-cookie")
				.stream()
				.filter(cookie -> Arrays.stream(searchCookieNames).anyMatch(searchCookie -> cookie.startsWith(searchCookie + '=')))
				.map(wholeCookie -> wholeCookie.substring(0, wholeCookie.indexOf(';')))
				.collect(Collectors.toSet());
	}

	private Collection<String> extractCookiesWithValuesFromHeaders(final HttpHeaders headers)
	{
		return headers.get("set-cookie")
				.stream()
				.map(wholeCookie -> wholeCookie.substring(0, wholeCookie.indexOf(';'))) //TODO what's that with ; and lats cookie in the string?
				.collect(Collectors.toSet());
	}

	private String findContractNumber()
	{
		//TODO make normal extraction
		return "207239";
	}

	private String extractJsonString(final String str)
	{
		final int indexOfStartJsonBody = str.indexOf('{');
		final int indexOfEndJsonBody = str.lastIndexOf('}');

		return indexOfEndJsonBody == -1 || indexOfStartJsonBody == -1 ?
				"{}" :
				str.substring(indexOfStartJsonBody, indexOfEndJsonBody + 1);
	}
}
