package ca.vastier.activityinscriptor.tasks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.JSONPObject;
import lombok.Setter;
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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class InscriptionTask implements Task
{
	private final RestTemplate restTemplate;
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
		final Collection<String> cookiesToPass = extractCookiesWithValuesFromHeaders(authenticationResponse.getHeaders());

		if (isPositiveLogin(authenticationResponse))
		{
			final ResponseEntity<String> bookingResponse = reserveClass(parameters, authenticationResponse);

			if (isPositiveClassBooking(bookingResponse))
			{


				final StringBuilder uriSb = new StringBuilder();
				uriSb.append("https://");
				uriSb.append((String)parameters.get("domain"));
				uriSb.append(".");
				uriSb.append(mainUrl);
				uriSb.append("/");
				uriSb.append(bookingConfirmUrl);

				final HttpHeaders confirmBookingHeaders = new HttpHeaders();
				confirmBookingHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
				confirmBookingHeaders.add("x-requested-with", "XMLHttpRequest");

				cookiesToPass.forEach(cookie -> confirmBookingHeaders.add("cookie", cookie));

				final MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
				map.add("id", (String)parameters.get("eventId"));
				map.add("dt", (String)parameters.get("dateTime"));
				map.add("contract_id", findContractNumber());
				map.add("present_number", (String)parameters.get("present_number")); //not mandatory. null is 1

				final HttpEntity<MultiValueMap<String, String>> bookingRequest = new HttpEntity<>(map, confirmBookingHeaders);

				final ResponseEntity<String> confirmationResponse = restTemplate.postForEntity(uriSb.toString(), bookingRequest, String.class);

				System.out.println(confirmationResponse.getStatusCodeValue());
			}
			System.out.println(bookingResponse.getStatusCodeValue());
		}
		System.out.println(authenticationResponse.getStatusCodeValue());
	}

	private ResponseEntity<String> reserveClass(final Map<String, Object> parameters,
			final ResponseEntity<String> authenticationResponse)
	{
		final StringBuilder uriSb = new StringBuilder();
		uriSb.append("https://");
		uriSb.append((String)parameters.get("domain"));
		uriSb.append(".");
		uriSb.append(mainUrl);
		uriSb.append("/");
		uriSb.append(registerClassUrl);

		final HttpHeaders bookingHeaders = new HttpHeaders();
		bookingHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		bookingHeaders.add("x-requested-with", "XMLHttpRequest");

		//final Collection<String> cookiesToPass = extractCookiesWithValuesFromHeaders(authenticationResponse.getHeaders(), "AWSALB", "PHPSESSID");
		final Collection<String> cookiesToPass = extractCookiesWithValuesFromHeaders(authenticationResponse.getHeaders());
		cookiesToPass.forEach(cookie -> bookingHeaders.add("cookie", cookie));

		final MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("id", (String)parameters.get("eventId"));
		map.add("dt", (String)parameters.get("dateTime"));
		//map.add("present_number", (String)parameters.get("present_number")); //not mandatory. null is 1

		final HttpEntity<MultiValueMap<String, String>> bookingRequest = new HttpEntity<>(map, bookingHeaders);

		return restTemplate.postForEntity(uriSb.toString(), bookingRequest, String.class);
	}

	private ResponseEntity<String> authenticate(final Map<String, Object> parameters)
	{
		final StringBuilder uriSb = new StringBuilder();
		uriSb.append("https://");
		uriSb.append((String)parameters.get("domain"));
		uriSb.append(".");
		uriSb.append(mainUrl);
		uriSb.append("/");
		uriSb.append(loginUrl);

		final HttpHeaders loginHeaders = new HttpHeaders();
		loginHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		final MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("username", (String)parameters.get("username"));
		map.add("password", (String)parameters.get("password"));

		final HttpEntity<MultiValueMap<String, String>> loginRequest = new HttpEntity<>(map, loginHeaders);

		return restTemplate.postForEntity(uriSb.toString(), loginRequest, String.class);
	}

	private boolean isPositiveLogin(final ResponseEntity<String> responseEntity)
	{
		if (HttpStatus.FOUND.equals(responseEntity.getStatusCode()) && responseEntity.getHeaders().get("Location").stream().anyMatch(str -> str.contains("/home/dashboard")))
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	private boolean isPositiveClassBooking(final ResponseEntity<String> responseEntity)
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
				e.printStackTrace();
			}

			if (!objectNode.get("error").booleanValue())
			{
				return true;
			}
		}

		return false;
	}

	private Collection<String> extractCookiesWithValuesFromHeaders(final HttpHeaders headers, final String... searchCookieNames)
	{
		return headers.get("set-cookie").stream()
				.filter(cookie -> Arrays.stream(searchCookieNames).anyMatch(searchCookie -> cookie.startsWith(searchCookie + '=')))
				.map(wholeCookie -> wholeCookie.substring(0, wholeCookie.indexOf(';')))
				.collect(Collectors.toSet());
	}

	private Collection<String> extractCookiesWithValuesFromHeaders(final HttpHeaders headers)
	{
		return headers.get("set-cookie").stream()
				.map(wholeCookie -> wholeCookie.substring(0, wholeCookie.indexOf(';')))
				.collect(Collectors.toSet());
	}

	private String findContractNumber()
	{
		return "207239";
	}
}
