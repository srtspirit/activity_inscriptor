package ca.vastier.activityinscriptor.services;

import ca.vastier.activityinscriptor.persistence.daos.CredentialDao;
import ca.vastier.activityinscriptor.persistence.entities.CredentialEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Setter;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static java.util.Optional.ofNullable;

//TODO refactor this class. Make it more generic. I see two options. Have a base class with forward request method and have a detailed implementation in descendants. The second option is to make this class configurable with rules and actions
public class WebSurfer
{
	private static final Logger LOGGER = LoggerFactory.getLogger(WebSurfer.class);

	private final RestTemplate restTemplate;
	private final FileReader fileReader;
	private final CredentialDao credentialDao;
	private final String baseServerUrl;
	private final ObjectMapper objectMapper;
	private final Clock utcClock;

	@Value("${web.surf.application.address}")
	@Setter
	private String surfApplicationAddress;

	public WebSurfer(final RestTemplate restTemplate, final FileReader fileReader, final CredentialDao credentialDao,
			final String baseServerUrl, @Qualifier("utcClock") final Clock utcClock)
	{
		this.restTemplate = restTemplate;
		this.fileReader = fileReader;
		this.credentialDao = credentialDao;
		this.baseServerUrl = baseServerUrl;
		this.utcClock = utcClock;
		this.objectMapper = new ObjectMapper();
	}

	public ResponseEntity<String> getDashboard(final MultiValueMap<String, String> headers)
	{
		final HttpResponseWrapper responseWrapper = forwardRequest(HttpMethod.GET.name(), "home/dashboard", headers, null);

		final Document domBody;
		try
		{
			//TODO normal exception and explanation
			domBody = responseWrapper.domBody.orElseThrow(() -> new RuntimeException());
			domBody.select("script")
					.stream()
					.filter(script -> script.attr("src").contains("home__dashboard_user"))
					.findFirst()
					.orElseGet(() -> domBody.createElement("script"))
					.attr("src", surfApplicationAddress + "file/js/home__dashboard_user.js");
		}
		catch (final RuntimeException e)
		{
			LOGGER.warn("Could not find parsed body");
			return responseWrapper.originalResponse;
		}

		return ResponseEntity.status(responseWrapper.httpStatus)
				.headers(responseWrapper.headers)
				.body(Parser.unescapeEntities(domBody.html(), true));
	}

	public <T> ResponseEntity<Object> surf(final String method, final String url, final MultiValueMap<String, String> headers,
			final T body)
	{
		final HttpResponseWrapper responseWrapper = forwardRequest(method, url, headers, body);
		final ResponseEntity.BodyBuilder builder = ResponseEntity.status(responseWrapper.httpStatus);
		builder.headers(responseWrapper.headers);

		final ResponseEntity<Object> responseEntity;

		if (responseWrapper.domBody.isPresent())
		{
			responseEntity = builder.body(Parser.unescapeEntities(responseWrapper.domBody.get().toString(), true));
		}
		else if (responseWrapper.jsonBody.isPresent())
		{
			responseEntity = builder.body(responseWrapper.jsonBody);
		}
		else
		{
			responseEntity = builder.body(responseWrapper.originalResponse.getBody());
		}

		return responseEntity;
	}

	public <T> ResponseEntity<String> login(final MultiValueMap<String, String> headers, final String body)
	{
		final HttpResponseWrapper responseWrapper = forwardRequest("POST", "home/login", headers, body);

		if (HttpStatus.FOUND.equals(responseWrapper.httpStatus))
		{
			final String[] bodyParams = body.split("\\&");
			final String login = bodyParams[0].substring(bodyParams[0].indexOf("=") + 1);
			final String password = bodyParams[1].substring(bodyParams[1].indexOf("=") + 1);

			final String cookieHeader = responseWrapper.headers.get("set-cookie")
					.stream()
					.filter(cookie -> cookie.startsWith("ci_session"))
					.findFirst()
					.orElse("");
			final HttpCookie ciSessionCookie = HttpCookie.parse(cookieHeader).get(0);
			final CredentialEntity entity = CredentialEntity.builder()
					.cookieValue(ciSessionCookie.getValue())
					.login(URLDecoder.decode(login, StandardCharsets.UTF_8))
					.password(URLDecoder.decode(password, StandardCharsets.UTF_8))
					.expirationDate(LocalDateTime.now(utcClock).plusSeconds(ciSessionCookie.getMaxAge()))
					.build();
			credentialDao.save(entity);
		}

		return new ResponseEntity<>(responseWrapper.originalResponse.getBody(), responseWrapper.headers, responseWrapper.httpStatus);
	}

	private <T> HttpResponseWrapper forwardRequest(final String method, final String url, final MultiValueMap<String, String> headers,
			final T body)
	{
		headers.remove("accept-encoding");
		//TODO set good headers. Tale paths from existing ones
		headers.set("host", baseServerUrl);
		headers.set("origin", baseServerUrl);
		headers.set("referer", baseServerUrl);

		final String remoteUrl = baseServerUrl + url;
		final ResponseEntity<String> responseEntity = restTemplate.exchange(remoteUrl, HttpMethod.valueOf(method),
				new HttpEntity<>(body, headers), String.class);

		LOGGER.info("forwarded {} request to {}. Got response {}", method, remoteUrl, responseEntity.getStatusCodeValue());
		LOGGER.debug("response body is {}", responseEntity.getBody());

		Document responseDocument = null;
		JsonNode responseJson = null;

		final HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.putAll(responseEntity.getHeaders()); //the ones inside the response are unmodifiable
		responseHeaders.remove("Transfer-Encoding");

		final Optional<List<String>> contentTypes = ofNullable(responseEntity.getHeaders().get(HttpHeaders.CONTENT_TYPE));

		if (responseEntity.getBody() != null && contentTypes.orElseGet(List::of)
				.stream()
				.anyMatch(contentType -> contentType.contains(MediaType.TEXT_HTML_VALUE)))
		{
			try
			{
				responseDocument = Parser.parse(responseEntity.getBody(), baseServerUrl);
				removeHyperlinks(responseDocument);
				changeBaseHrefAfterPageIsLoaded(responseDocument);
				addDisclaimer(responseDocument);
			}
			catch (final Exception e)
			{
				LOGGER.warn("Could not parse document: {}", e.getMessage());
			}
		}
		else if (contentTypes.orElseGet(List::of).stream().anyMatch(MediaType.APPLICATION_JSON_VALUE::equals))
		{
			try
			{
				responseJson = objectMapper.readTree(responseEntity.getBody());
			}
			catch (JsonProcessingException e)
			{
				LOGGER.warn("Could not parse json node: {}", e.getMessage());
			}
		}

		if (HttpStatus.FOUND.equals(responseEntity.getStatusCode()))
		{
			try
			{
				final String oldLocation = responseEntity.getHeaders().getLocation().toString();
				final String newLocation = oldLocation.replaceFirst("https?:\\/\\/[^\\/]+\\/", surfApplicationAddress);

				responseHeaders.setLocation(new URI(newLocation));
			}
			catch (final Exception e)
			{
				LOGGER.warn("Could not change location in the request: {}", e.getMessage());
			}
		}

		return HttpResponseWrapper.builder()
				.domBody(ofNullable(responseDocument))
				.headers(responseHeaders)
				.jsonBody(ofNullable(responseJson))
				.httpStatus(responseEntity.getStatusCode())
				.originalResponse(responseEntity)
				.build();
	}

	private void removeHyperlinks(final Document document)
	{
		document.select("a").forEach(a -> a.removeAttr("href"));
		document.select("button")
				.stream()
				.filter(button -> button.attr("onclick").contains("document.location"))
				.forEach(Node::remove);
	}

	private void changeBaseHrefAfterPageIsLoaded(final Document document)
	{
		final String fileName = "templates/change-base-href-after-page-is-loaded.js";

		try
		{
			final String scriptText = fileReader.readFileAsString(fileName);
			final Element script = document.createElement("script");
			script.html(String.format(scriptText, "\"" + surfApplicationAddress + "\""));
			document.body().appendChild(script);
		}
		catch (IOException e)
		{
			LOGGER.warn("Could not read file {}: {}\n Impossible to change the base href", fileName, e.getMessage());
		}
	}

	private void addDisclaimer(final Document document)
	{
		final String templateFileName = "templates/disclaimer.html";
		final String styleFileName = "templates/styles.css";

		try
		{
			final String templateContent = fileReader.readFileAsString(templateFileName);
			final Element div = document.createElement("div");
			div.html(templateContent);
			document.body()
					.appendChild(div.child(
							0)); //use children and wrapper in order to keep the entire html in the file. Otherwise I would have to set up classes manually in java

			final String styleContent = fileReader.readFileAsString(styleFileName);
			final Element style = document.createElement("style");
			style.html(styleContent);
			document.body().appendChild(style);
		}
		catch (IOException e)
		{
			LOGGER.warn("Could not read file {}: {}\n Impossible to add a disclaimer", templateFileName, e.getMessage());
		}
	}

	@AllArgsConstructor
	@Builder
	private static class HttpResponseWrapper
	{
		Optional<JsonNode> jsonBody;
		Optional<Document> domBody;
		HttpHeaders headers;
		HttpStatus httpStatus;
		ResponseEntity<String> originalResponse;
	}
}
