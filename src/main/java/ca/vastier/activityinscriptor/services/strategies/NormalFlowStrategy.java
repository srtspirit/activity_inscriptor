package ca.vastier.activityinscriptor.services.strategies;

import ca.vastier.activityinscriptor.services.WebSurfer2;
import org.springframework.web.client.RestTemplate;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class NormalFlowStrategy extends Strategy
{
	private Consumer<WebSurfer2.HttpRequestWrapper> requestTransformations;
	private Consumer<WebSurfer2.HttpResponseWrapper> responseTransformations;

	public NormalFlowStrategy(final Predicate<WebSurfer2.HttpRequestWrapper> requestPredicate,
			final Consumer<WebSurfer2.HttpRequestWrapper> requestTransformations)
	{
		this.requestPredicate = requestPredicate;
		this.requestTransformations = requestTransformations;
	}

	public NormalFlowStrategy(final Predicate<WebSurfer2.HttpRequestWrapper> requestPredicate,
			final Consumer<WebSurfer2.HttpRequestWrapper> requestTransformations,
			final Predicate<WebSurfer2.HttpResponseWrapper> responsePredicate,
			final Consumer<WebSurfer2.HttpResponseWrapper> responseTransformations)
	{
		this(requestPredicate, requestTransformations);
		this.responsePredicate = responsePredicate;
		this.responseTransformations = responseTransformations;
	}

	@Override
	public WebSurfer2.HttpResponseWrapper surf(final WebSurfer2.HttpRequestWrapper request, final RestTemplate restTemplate)
	{
		if (requestPredicate.test(request))
		{
			if (requestTransformations != null)
			{
				requestTransformations.accept(request);
			}

			final WebSurfer2.HttpResponseWrapper response = callNext(request, restTemplate);

			if (responsePredicate != null && responsePredicate.test(response))
			{
				responseTransformations.accept(response);
			}

			return response;
		}
		else
		{
			return callNext(request, restTemplate);
		}
	}
}
