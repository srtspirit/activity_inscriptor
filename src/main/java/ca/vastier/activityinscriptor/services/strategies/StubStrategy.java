package ca.vastier.activityinscriptor.services.strategies;

import ca.vastier.activityinscriptor.services.WebSurfer2;
import lombok.EqualsAndHashCode;
import org.springframework.web.client.RestTemplate;

import java.util.function.Predicate;

public class StubStrategy extends Strategy
{
	private WebSurfer2.HttpResponseWrapper responseWrapper;

	@Override
	public WebSurfer2.HttpResponseWrapper surf(final WebSurfer2.HttpRequestWrapper requestWrapper, final RestTemplate restTemplate)
	{
		return responseWrapper;
	}

	public StubStrategy(final Predicate<WebSurfer2.HttpRequestWrapper> requestPredicate,
			final WebSurfer2.HttpResponseWrapper responseWrapper)
	{
		this.requestPredicate = requestPredicate;
		this.responseWrapper = responseWrapper;
	}
}
