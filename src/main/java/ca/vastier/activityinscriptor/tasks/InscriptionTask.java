package ca.vastier.activityinscriptor.tasks;

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

import static java.util.Optional.ofNullable;

@Component
public class InscriptionTask implements Task
{
	private final static Logger LOGGER = LoggerFactory.getLogger(InscriptionTask.class);

	//TODO decouple spring's rest template
	private final RestTemplate restTemplate;

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

	@Autowired
	public InscriptionTask(final RestTemplate restTemplate)
	{
		this.restTemplate = restTemplate;
	}

	public void run(final Map<String, Object> parameters)
	{
		final ResponseEntity<String> authenticationResponse = authenticate(parameters);
		LOGGER.info("sent authentification request... Responded with status code: {}. The response body: {}",
				authenticationResponse.getStatusCodeValue(), authenticationResponse.getBody());
		assertSuccessfulLogin(authenticationResponse);
		final Collection<String> cookiesToPass = extractCookiesWithValuesFromHeaders(authenticationResponse.getHeaders());

		ResponseEntity<String> bookingResponse;
		final long delayBetweenTries = ofNullable(Long.parseLong((String) parameters.get("delay"))).orElse(1000L);
		do
		{
			LOGGER.info("The inscription has not started yet"); //TODO on the first iteration this message might be wrong
			try
			{
				Thread.sleep(delayBetweenTries);
			}
			catch (InterruptedException e)
			{
			}
			bookingResponse = bookPlaceInEvent(parameters, cookiesToPass);
			LOGGER.info("sent booking request... Responded with status code: {}. The response body: {}",
					bookingResponse.getStatusCodeValue(), bookingResponse.getBody());
		}
		while (inscriptionIsNotYetOpen(bookingResponse));
		assertSuccessfulBooking(bookingResponse);

		final ResponseEntity<String> confirmationResponse = confirmBooking(parameters, cookiesToPass);
		LOGGER.info("sent booking confirmation... Responded with status code: {}. The response body: {}",
				confirmationResponse.getStatusCodeValue(), confirmationResponse.getBody());
		assertSuccessfulConfirmation(confirmationResponse);
	}

	private ResponseEntity<String> confirmBooking(final Map<String, Object> parameters, final Collection<String> cookiesToPass)
	{
		final StringBuilder uriSb = new StringBuilder();
		uriSb.append("https://");
		uriSb.append((String) parameters.get("domain"));
		uriSb.append(".");
		uriSb.append(mainUrl);
		uriSb.append("/");
		uriSb.append(bookingConfirmUrl);

		final HttpHeaders confirmBookingHeaders = new HttpHeaders();
		confirmBookingHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		confirmBookingHeaders.add("x-requested-with", "XMLHttpRequest");

		cookiesToPass.forEach(cookie -> confirmBookingHeaders.add("cookie", cookie));

		final MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("id", (String) parameters.get("eventId"));
		map.add("dt", (String) parameters.get("date"));
		map.add("contract_id", findContractNumber());
		map.add("present_number", (String) parameters.get("visitors")); //not mandatory. null is 1

		final HttpEntity<MultiValueMap<String, String>> bookingRequest = new HttpEntity<>(map, confirmBookingHeaders);

		return restTemplate.postForEntity(uriSb.toString(), bookingRequest, String.class);
	}

	private ResponseEntity<String> bookPlaceInEvent(final Map<String, Object> parameters, final Collection<String> cookiesToPass)
	{
		final StringBuilder uriSb = new StringBuilder();
		uriSb.append("https://");
		uriSb.append((String) parameters.get("domain"));
		uriSb.append(".");
		uriSb.append(mainUrl);
		uriSb.append("/");
		uriSb.append(registerClassUrl);

		final HttpHeaders bookingHeaders = new HttpHeaders();
		bookingHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		bookingHeaders.add("x-requested-with", "XMLHttpRequest");

		cookiesToPass.forEach(cookie -> bookingHeaders.add("cookie", cookie));

		final MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("id", (String) parameters.get("eventId"));
		map.add("dt", (String) parameters.get("date"));
		//map.add("present_number", (String)parameters.get("present_number")); //not mandatory. null is 1

		final HttpEntity<MultiValueMap<String, String>> bookingRequest = new HttpEntity<>(map, bookingHeaders);

		return restTemplate.postForEntity(uriSb.toString(), bookingRequest, String.class);
	}

	private ResponseEntity<String> authenticate(final Map<String, Object> parameters)
	{
		final StringBuilder uriSb = new StringBuilder();
		uriSb.append("https://");
		uriSb.append((String) parameters.get("domain"));
		uriSb.append(".");
		uriSb.append(mainUrl);
		uriSb.append("/");
		uriSb.append(loginUrl);

		final HttpHeaders loginHeaders = new HttpHeaders();
		loginHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		final MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("username", (String) parameters.get("username"));
		map.add("password", (String) parameters.get("password"));

		final HttpEntity<MultiValueMap<String, String>> loginRequest = new HttpEntity<>(map, loginHeaders);

		return restTemplate.postForEntity(uriSb.toString(), loginRequest, String.class);
	}

	private void assertSuccessfulLogin(final ResponseEntity<String> responseEntity)
	{
		if (!HttpStatus.FOUND.equals(responseEntity.getStatusCode()) || !responseEntity.getHeaders()
				.get("Location")
				.stream()
				.anyMatch(str -> str.contains("/home/dashboard")))
		{
			//TODO create good exception and pass error description
			throw new RuntimeException("login failure");
		}
	}

	private void assertSuccessfulBooking(final ResponseEntity<String> responseEntity)
	{

		if (HttpStatus.OK.equals(responseEntity.getStatusCode()))
		{
			final ObjectMapper objectMapper = new ObjectMapper();
			ObjectNode objectNode = null;
			try
			{
				objectNode = objectMapper.readValue(responseEntity.getBody(), ObjectNode.class);
			}
			catch (JsonProcessingException e)
			{
				//TODO create a good exception
				e.printStackTrace();
			}

			if (objectNode.get("error").booleanValue())
			{
				//TODO create a good exception
				throw new RuntimeException("booking failure");
			}
		}
		else
		{
			//TODO create a good exception
			throw new RuntimeException("booking failure");
		}
	}

	private void assertSuccessfulConfirmation(final ResponseEntity<String> responseEntity)
	{

		if (HttpStatus.OK.equals(responseEntity.getStatusCode()))
		{
			final ObjectMapper objectMapper = new ObjectMapper();
			ObjectNode objectNode = null;
			try
			{
				objectNode = objectMapper.readValue(responseEntity.getBody(), ObjectNode.class);
			}
			catch (JsonProcessingException e)
			{
				//TODO create a good exception
				e.printStackTrace();
			}

			if (objectNode.get("error").booleanValue())
			{
				//TODO create a good exception
				throw new RuntimeException("booking failure");
			}
		}
		else
		{
			//TODO create a good exception
			throw new RuntimeException("booking failure");
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
				objectNode = objectMapper.readValue(responseEntity.getBody(), ObjectNode.class);
			}
			catch (JsonProcessingException e)
			{
				return false;
			}

			return objectNode.get("error").booleanValue() && objectNode.get("message")
					.asText()
					.contains("cours plus de 2 jour(s) (48:00 heures) avant");
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
				.map(wholeCookie -> wholeCookie.substring(0, wholeCookie.indexOf(';')))
				.collect(Collectors.toSet());
	}

	private String findContractNumber()
	{
		//TODO make normal extraction
		return "207239";
	}
}
