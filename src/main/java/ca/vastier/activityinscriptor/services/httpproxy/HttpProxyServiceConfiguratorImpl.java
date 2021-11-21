package ca.vastier.activityinscriptor.services.httpproxy;

import lombok.NonNull;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ca.vastier.activityinscriptor.services.httpproxy.HttpProxyService.HttpRequestWrapper;
import static ca.vastier.activityinscriptor.services.httpproxy.HttpProxyService.HttpResponseWrapper;
import static ca.vastier.activityinscriptor.services.httpproxy.HttpProxyService.RequestConditionConfigurator;
import static ca.vastier.activityinscriptor.services.httpproxy.HttpProxyService.RequestTransformationConfigurator;
import static ca.vastier.activityinscriptor.services.httpproxy.HttpProxyService.ResponseConditionConfigurator;
import static ca.vastier.activityinscriptor.services.httpproxy.HttpProxyService.ResponseTransformationConfigurator;
import static ca.vastier.activityinscriptor.services.httpproxy.HttpProxyService.WebSurferConfigurator;
import static ca.vastier.activityinscriptor.services.httpproxy.HttpProxyService.WebSurferRule;
import static java.util.Optional.ofNullable;

public class HttpProxyServiceConfiguratorImpl
		implements WebSurferConfigurator, RequestConditionConfigurator, RequestTransformationConfigurator,
		ResponseConditionConfigurator, ResponseTransformationConfigurator
{
	private static final Logger log = LoggerFactory.getLogger(HttpProxyService.class);

	private final HttpRequestExecutor httpRequestExecutor;

	private final List<WebSurferRule> rules = new ArrayList<>();
	private String ruleName;

	private Predicate<HttpRequestWrapper> requestPredicate;
	private Consumer<HttpRequestWrapper> requestTransformations;
	private Predicate<HttpResponseWrapper> responsePredicate;
	private Consumer<HttpResponseWrapper> responseTransformations;

	HttpProxyServiceConfiguratorImpl(final HttpRequestExecutor httpRequestExecutor)
	{
		this.httpRequestExecutor = httpRequestExecutor;
		init();
	}

	private void init()
	{
		ruleName = null;
		requestPredicate = req -> true;
		requestTransformations = null;
		responsePredicate = res -> true;
		responseTransformations = null;
	}

	private void flush()
	{
		if (requestTransformations != null || responseTransformations != null)
		{
			rules.add(WebSurferRule.builder()
					.name(ofNullable(ruleName).orElseGet(this::generateDefaultRuleName))
					.requestPredicate(requestPredicate)
					.requestTransformation(requestTransformations)
					.responsePredicate(responsePredicate)
					.responseTransformation(responseTransformations)
					.build());
			init();
		}
	}

	private String generateDefaultRuleName()
	{
		//TODO think about suitable default naming
		return "noName";
	}

	private void saveRequestPredicate(final Predicate<HttpRequestWrapper> predicate)
	{
		requestPredicate = requestPredicate.and(predicate);
	}

	private void saveResponsePredicate(final Predicate<HttpResponseWrapper> predicate)
	{
		responsePredicate = responsePredicate.and(predicate);
	}

	private void saveResponseTransformations(final Consumer<HttpResponseWrapper> transformations)
	{
		responseTransformations = responseTransformations == null ? transformations : responseTransformations.andThen(transformations);
	}

	private Predicate<HttpRequestWrapper> createPredicateForHttpMethod(final String method)
	{
		return requestWrapper -> ofNullable(requestWrapper).map(HttpRequestWrapper::getMethod)
				.map(m -> method.equals(m))
				.orElse(false);
	}

	private Predicate<HttpRequestWrapper> createPredicateForUrlMatching(final String urlPattern)
	{
		//TODO make use of URL wildcards
		final Pattern regexPattern = Pattern.compile(urlPattern);
		return requestWrapper -> ofNullable(requestWrapper.getUrl()).map(url -> regexPattern.matcher(url).find()).orElse(false);
	}

	@Override
	public RequestConditionConfigurator get(@NonNull final String url)
	{
		flush();
		Predicate<HttpRequestWrapper> predicate = createPredicateForHttpMethod("GET").and(createPredicateForUrlMatching(url));
		saveRequestPredicate(predicate);

		return this;
	}

	@Override
	public RequestConditionConfigurator get()
	{
		flush();
		Predicate<HttpRequestWrapper> predicate = createPredicateForHttpMethod("GET");
		saveRequestPredicate(predicate);

		return this;
	}

	@Override
	public RequestConditionConfigurator post(@NonNull final String url)
	{
		flush();
		Predicate<HttpRequestWrapper> predicate = createPredicateForHttpMethod("POST").and(createPredicateForUrlMatching(url));
		saveRequestPredicate(predicate);

		return this;
	}

	@Override
	public RequestConditionConfigurator post()
	{
		flush();
		Predicate<HttpRequestWrapper> predicate = createPredicateForHttpMethod("POST");
		saveRequestPredicate(predicate);

		return this;
	}

	@Override
	public RequestConditionConfigurator put(@NonNull final String url)
	{
		flush();
		Predicate<HttpRequestWrapper> predicate = createPredicateForHttpMethod("PUT").and(createPredicateForUrlMatching(url));
		saveRequestPredicate(predicate);

		return this;
	}

	@Override
	public RequestConditionConfigurator put()
	{
		flush();
		Predicate<HttpRequestWrapper> predicate = createPredicateForHttpMethod("PUT");
		saveRequestPredicate(predicate);

		return this;
	}

	@Override
	public RequestConditionConfigurator delete(@NonNull final String url)
	{
		flush();
		Predicate<HttpRequestWrapper> predicate = createPredicateForHttpMethod("DELETE").and(createPredicateForUrlMatching(url));
		saveRequestPredicate(predicate);

		return this;
	}

	@Override
	public RequestConditionConfigurator delete()
	{
		flush();
		Predicate<HttpRequestWrapper> predicate = createPredicateForHttpMethod("DELETE");
		saveRequestPredicate(predicate);

		return this;
	}

	@Override
	public RequestConditionConfigurator requestHasHeader(@NonNull final String header)
	{
		flush();
		Predicate<HttpRequestWrapper> head = requestWrapper -> ofNullable(requestWrapper).map(HttpRequestWrapper::getHeaders)
				.map(h -> h.containsKey(header))
				.orElse(false);

		saveRequestPredicate(head);

		return this;
	}

	@Override
	public RequestConditionConfigurator requestHasNoHeader(@NonNull final String header)
	{
		flush();
		Predicate<HttpRequestWrapper> head = requestWrapper -> ofNullable(requestWrapper).map(HttpRequestWrapper::getHeaders)
				.map(h -> h.get(header) == null)
				.orElse(true);

		saveRequestPredicate(head);

		return this;
	}

	@Override
	public RequestConditionConfigurator requestHasHeaderWithValue(@NonNull final String header, @NonNull final String value)
	{
		flush();
		final String upperCaseValue = value.toUpperCase();
		Predicate<HttpRequestWrapper> head = requestWrapper -> ofNullable(requestWrapper).map(HttpRequestWrapper::getHeaders)
				.map(h -> h.get(header))
				.filter(Objects::nonNull)
				.map(headerValues -> headerValues.stream().filter(Objects::nonNull).anyMatch(val -> val.toUpperCase().contains(upperCaseValue)))
				.orElse(false);

		saveRequestPredicate(head);

		return this;
	}

	@Override
	public RequestConditionConfigurator anyRequest()
	{
		flush();
		requestPredicate = req -> true;

		return this;
	}

	@Override
	public RequestTransformationConfigurator setRequestHeader(@NonNull final String name, @NonNull final String value)
	{
		final Consumer<HttpRequestWrapper> cons = requestWrapper -> {
			if (requestWrapper.getHeaders() == null)
			{
				requestWrapper.setHeaders(new HttpHeaders());
			}
			requestWrapper.getHeaders().set(name, value);
		};

		requestTransformations = requestTransformations == null ? cons : requestTransformations.andThen(cons);

		return this;
	}

	@Override
	public RequestTransformationConfigurator removeHeaderFromRequest(@NonNull final String name)
	{
		final Consumer<HttpRequestWrapper> cons = requestWrapper -> ofNullable(requestWrapper.getHeaders()).ifPresent(
				headers -> headers.remove(name));

		requestTransformations = requestTransformations == null ? cons : requestTransformations.andThen(cons);

		return this;
	}

	@Override
	public RequestTransformationConfigurator changeUrl(final String newUrl)
	{
		final Consumer<HttpRequestWrapper> cons = requestWrapper -> requestWrapper.setUrl(newUrl);

		requestTransformations = requestTransformations == null ? cons : requestTransformations.andThen(cons);

		return this;
	}

	@Override
	public RequestTransformationConfigurator changeUrl(final Function<HttpRequestWrapper, String> urlValueEvaluator)
	{
		final Consumer<HttpRequestWrapper> cons = requestWrapper -> requestWrapper.setUrl(urlValueEvaluator.apply(requestWrapper));

		requestTransformations = requestTransformations == null ? cons : requestTransformations.andThen(cons);

		return this;
	}

	@Override
	public RequestTransformationConfigurator customRequestTransformation(@NonNull final Consumer<HttpRequestWrapper> consumer)
	{
		requestTransformations = requestTransformations == null ? consumer : requestTransformations.andThen(consumer);

		return this;
	}

	@Override
	public ResponseConditionConfigurator anyResponse()
	{
		flush();
		responsePredicate = res -> true;

		return this;
	}

	@Override
	public ResponseConditionConfigurator responseHasHeader(@NonNull final String header)
	{
		flush();
		Predicate<HttpResponseWrapper> head = responseWrapper -> responseWrapper.getHeaders().containsKey(header);
		saveResponsePredicate(head);

		return this;
	}

	@Override
	public ResponseConditionConfigurator responseHasHeaderWithValue(@NonNull final String header, @NonNull final String value)
	{
		flush();

		final String upperCaseValue = value.toUpperCase();
		Predicate<HttpResponseWrapper> head = responseWrapper -> ofNullable(responseWrapper).map(HttpResponseWrapper::getHeaders)
				.map(h -> h.get(header))
				.filter(Objects::nonNull)
				.map(headerValues -> headerValues.stream().filter(Objects::nonNull).anyMatch(val -> val.toUpperCase().contains(upperCaseValue)))
				.orElse(false);

		saveResponsePredicate(head);

		return this;
	}

	@Override
	public ResponseConditionConfigurator responseHasNoHeader(@NonNull final String header)
	{
		flush();
		final Predicate<HttpResponseWrapper> head = responseWrapper -> ofNullable(responseWrapper).map(HttpResponseWrapper::getHeaders)
				.map(h -> h.get(header) == null)
				.orElse(true);

		saveResponsePredicate(head);

		return this;
	}

	@Override
	public ResponseConditionConfigurator hasStatus(final int statusCode)
	{
		flush();
		final Predicate<HttpResponseWrapper> status = responseWrapper -> responseWrapper.getHttpStatus().value() == statusCode;
		saveResponsePredicate(status);

		return this;
	}

	@Override
	public ResponseTransformationConfigurator appendHtmlElementToBody(final String tagName, final Supplier<String> htmlContentSupplier)
	{
		final Consumer<HttpResponseWrapper> consumer = responseWrapper -> {
			if (responseWrapper.getDomBody() == null)
			{
				if (responseWrapper.getOriginalResponse() != null && responseWrapper.getOriginalResponse().getBody() != null)
				{
					responseWrapper.setDomBody(Parser.parse(responseWrapper.getOriginalResponse().getBody(),
							responseWrapper.getOriginalRequest().getUrl()));
				}
				else
				{
					log.warn("tried to alternate DOM in the response but it has no body");
					return;
				}
			}
			if (responseWrapper.getDomBody() != null)
				try
				{
					final Element div = responseWrapper.getDomBody().createElement(tagName);
					//TODO exception when supplier throws file not found. And when tagName is wrong
					div.html(htmlContentSupplier.get());
					responseWrapper.getDomBody().body().appendChild(div);

				}
				catch (final RuntimeException e)
				{
					log.warn("an error occurred when appending html element: {}", e.getMessage());
				}
		};

		saveResponseTransformations(consumer);

		return this;
	}

	@Override
	public ResponseTransformationConfigurator removeHeaderFromResponse(final String name)
	{
		Consumer<HttpResponseWrapper> consumer = responseWrapper -> ofNullable(responseWrapper.getHeaders()).ifPresent(
				headers -> headers.remove(name));

		saveResponseTransformations(consumer);

		return this;
	}

	@Override
	public ResponseTransformationConfigurator setResponseHeader(final String name, final String value)
	{
		final Consumer<HttpResponseWrapper> consumer = responseWrapper -> {
			if (responseWrapper.getHeaders() == null)
			{
				responseWrapper.setHeaders(new HttpHeaders());
			}
			responseWrapper.getHeaders().set(name, value);
		};

		saveResponseTransformations(consumer);

		return this;
	}

	@Override
	public ResponseTransformationConfigurator setResponseHeader(final String name,
			final Function<HttpResponseWrapper, String> headerValueEvaluator)
	{
		final Consumer<HttpResponseWrapper> consumer = responseWrapper -> {
			if (responseWrapper.getHeaders() == null)
			{
				responseWrapper.setHeaders(new HttpHeaders());
			}
			responseWrapper.getHeaders().set(name, headerValueEvaluator.apply(responseWrapper));
		};

		saveResponseTransformations(consumer);

		return this;
	}

	@Override
	public ResponseTransformationConfigurator customResponseTransformation(final Consumer<HttpResponseWrapper> consumer)
	{
		saveResponseTransformations(consumer);
		return this;
	}

	@Override
	public ResponseTransformationConfigurator changeHtmlElement(final String tagName, final Predicate<Element> filter,
			final Consumer<Element> action)
	{
		final Consumer<HttpResponseWrapper> consumer = responseWrapper -> {
			if (responseWrapper.getDomBody() == null)
			{
				if (responseWrapper.getOriginalResponse() != null && responseWrapper.getOriginalResponse().getBody() != null)
				{
					responseWrapper.setDomBody(Parser.parse(responseWrapper.getOriginalResponse().getBody(),
							responseWrapper.getOriginalRequest().getUrl()));
				}
				else
				{
					log.warn("tried to alternate DOM in the response but it has no body");
					return;
				}
			}
			final List<Element> foundHtmlElements = responseWrapper.getDomBody()
					.select(tagName)
					.stream()
					.filter(filter)
					.collect(Collectors.toList());

			if (!foundHtmlElements.isEmpty())
			{
				if (foundHtmlElements.size() > 1)
				{
					//TODO better request identifier
					log.warn("found more than one element. Applying transformations to the first one");
				}

				action.accept(foundHtmlElements.get(0));
			}
			else
			{
				log.debug("html elements for this filter not found");
			}
		};

		saveResponseTransformations(consumer);
		return this;
	}

	@Override
	public ResponseTransformationConfigurator changeAllHtmlElements(final String tagName, final Predicate<Element> filter,
			final Consumer<Element> action)
	{
		final Consumer<HttpResponseWrapper> consumer = responseWrapper -> {
			if (responseWrapper.getDomBody() == null)
			{
				if (responseWrapper.getOriginalResponse() != null && responseWrapper.getOriginalResponse().getBody() != null)
				{
					responseWrapper.setDomBody(Parser.parse(responseWrapper.getOriginalResponse().getBody(),
							responseWrapper.getOriginalRequest().getUrl()));
				}
				else
				{
					log.warn("tried to alternate DOM in the response but it has no body");
					return;
				}
			}

			responseWrapper.getDomBody().select(tagName).stream().filter(filter).forEach(action::accept);
		};

		saveResponseTransformations(consumer);
		return this;
	}

	@Override
	public WebSurferConfigurator ruleName(final String name)
	{
		flush();
		this.ruleName = name;
		return this;
	}

	public HttpProxyService build()
	{
		flush();
		return new HttpProxyService(httpRequestExecutor, rules);
	}
}
