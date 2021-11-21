package ca.vastier.activityinscriptor.services.httpproxy;

import ca.vastier.activityinscriptor.services.httpproxy.HttpProxyService;
import ca.vastier.activityinscriptor.services.httpproxy.HttpRequestExecutor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

import static java.util.Optional.ofNullable;

@Slf4j
@Component
public class RestTemplateHttpRequestExecutorImpl implements HttpRequestExecutor
{
	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;

	public RestTemplateHttpRequestExecutorImpl(final RestTemplate restTemplate)
	{
		this.restTemplate = restTemplate;
		this.objectMapper = new ObjectMapper();
	}

	@Override
	public HttpProxyService.HttpResponseWrapper executeRequest(final HttpProxyService.HttpRequestWrapper request)
	{
		log.info("forwarded {} request to {}.", request.getMethod(), request.getUrl());
		//TODO handle exception
		final ResponseEntity<String> responseEntity = restTemplate.exchange(request.getUrl(), HttpMethod.valueOf(request.getMethod()),
				new HttpEntity<>(request.getBody(), request.getHeaders()), String.class);

		log.info("Got response {}", responseEntity.getStatusCodeValue());
		log.trace("response body is {}", responseEntity.getBody());

		final HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.putAll(responseEntity.getHeaders()); //the ones inside the response are unmodifiable

		return HttpProxyService.HttpResponseWrapper.builder()
				.headers(responseHeaders)
				.httpStatus(responseEntity.getStatusCode())
				.originalResponse(responseEntity)
				.originalRequest(request)
				.build();
	}
}
