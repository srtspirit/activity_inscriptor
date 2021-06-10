package ca.vastier.activityinscriptor.services.strategies;

import ca.vastier.activityinscriptor.services.WebSurfer2;
import ca.vastier.activityinscriptor.services.WebSurfer2.HttpRequestWrapper;
import ca.vastier.activityinscriptor.services.WebSurfer2.HttpResponseWrapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.function.Predicate;

public abstract class Strategy
{
	protected Predicate<HttpRequestWrapper> requestPredicate;
	protected Predicate<HttpResponseWrapper> responsePredicate;

	protected Strategy next;

	protected boolean hasNext()
	{
		return next != null;
	}

	protected void setNext(final Strategy nextStrategy)
	{
		this.next = nextStrategy;
	}

	protected HttpResponseWrapper callNext(final HttpRequestWrapper request, final RestTemplate restTemplate)
	{
		if (next != null)
		{
			return next.surf(request, restTemplate);
		}
		else
		{
			return executeRequest(request, restTemplate);
		}
	}

	abstract public HttpResponseWrapper surf(final HttpRequestWrapper request, final RestTemplate restTemplate);

	public Strategy andThen(final Strategy nextStrategy)
	{
		Strategy lastStrategy = this;

		while (lastStrategy.hasNext())
		{
			lastStrategy = lastStrategy.next;
		}

		lastStrategy.setNext(nextStrategy);

		return this;
	}

	protected HttpResponseWrapper executeRequest(final HttpRequestWrapper requestWrapper, final RestTemplate restTemplate)
	{
		//TODO decouple from rest template
		final ResponseEntity<String> responseEntity = restTemplate.exchange(requestWrapper.getUrl(),
				HttpMethod.valueOf(requestWrapper.getMethod()),
				new HttpEntity<>(requestWrapper.getBody(), requestWrapper.getHeaders()), String.class);
		WebSurfer2.HttpResponseWrapper.builder()
				.headers(responseEntity.getHeaders())
				.httpStatus(responseEntity.getStatusCode())
				.originalResponse(responseEntity)
				.build();
		//TODO implement
		return null;
	}
}
