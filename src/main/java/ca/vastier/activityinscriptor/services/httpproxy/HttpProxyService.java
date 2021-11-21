package ca.vastier.activityinscriptor.services.httpproxy;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
public class HttpProxyService
{
	private final HttpRequestExecutor httpRequestExecutor;
	private final List<WebSurferRule> rules;

	HttpProxyService(final HttpRequestExecutor httpRequestExecutor, final List<WebSurferRule> rules)
	{
		this.httpRequestExecutor = httpRequestExecutor;
		this.rules = rules;
	}

	public static WebSurferConfigurator configure(@NonNull final HttpRequestExecutor httpRequestExecutor)
	{
		return new HttpProxyServiceConfiguratorImpl(httpRequestExecutor);
	}

	public HttpResponseWrapper surf(@NonNull final HttpRequestWrapper requestWrapper)
	{
		final List<WebSurferRule> triggeredRulesByRequest = rules.stream()
				.filter(rule -> {
					final boolean result = rule.getRequestPredicate().test(requestWrapper);
					log.debug("Request testing rule \"{}\"... Passed: {}", rule.getName(), result);
					return result;
				})
				.collect(Collectors.toList());

		triggeredRulesByRequest.stream()
				.filter(rule -> rule.getRequestTransformation() != null)
				.peek(rule -> log.info("Rule \"{}\". Applying request transformations.", rule.getName()))
				.map(WebSurferRule::getRequestTransformation)
				.forEach(transformations -> transformations.accept(requestWrapper));

		final HttpResponseWrapper responseWrapper = httpRequestExecutor.executeRequest(requestWrapper);

		final List<WebSurferRule> triggeredRulesByResponse = triggeredRulesByRequest.stream()
				.filter(rule -> {
					boolean result = rule.getResponsePredicate().test(responseWrapper);
					log.debug("Response testing rule \"{}\"... Passed: {}", rule.getName(), result);
					return result;
				})
				.collect(Collectors.toList());

		triggeredRulesByResponse.stream()
				.filter(rule -> rule.getResponseTransformation() != null)
				.peek(rule -> log.info("Rule \"{}\". Applying response transformations.", rule.getName()))
				.map(WebSurferRule::getResponseTransformation)
				.forEach(transformations -> transformations.accept(responseWrapper));

		return responseWrapper;
	}

	public interface WebSurferConfigurator extends RequestCondition, ResponseCondition, Builder
	{
	}

	public interface RequestConditionConfigurator
			extends RequestCondition, RequestTransformation, ResponseCondition, ResponseTransformation
	{
	}

	public interface RequestTransformationConfigurator
			extends RequestCondition, RequestTransformation, ResponseCondition, ResponseTransformation, Builder
	{
	}

	public interface ResponseConditionConfigurator extends ResponseCondition, ResponseTransformation
	{
	}

	public interface ResponseTransformationConfigurator extends RequestCondition, ResponseCondition, ResponseTransformation, Builder
	{
	}

	public interface Builder
	{
		WebSurferConfigurator ruleName(String name);
		HttpProxyService build();
	}

	public interface RequestCondition
	{
		RequestConditionConfigurator get(String url);

		RequestConditionConfigurator get();

		RequestConditionConfigurator post(String url);

		RequestConditionConfigurator post();

		RequestConditionConfigurator put(String url);

		RequestConditionConfigurator put();

		RequestConditionConfigurator delete(String url);

		RequestConditionConfigurator delete();

		RequestConditionConfigurator requestHasHeader(String header);

		RequestConditionConfigurator requestHasNoHeader(String header);

		RequestConditionConfigurator requestHasHeaderWithValue(String header, String value);

		RequestConditionConfigurator anyRequest();
	}

	public interface RequestTransformation
	{
		RequestTransformationConfigurator setRequestHeader(String name, String value);

		RequestTransformationConfigurator removeHeaderFromRequest(String name);

		RequestTransformationConfigurator changeUrl(String newUrl);

		RequestTransformationConfigurator changeUrl(Function<HttpRequestWrapper, String> urlValueEvaluator);

		RequestTransformationConfigurator customRequestTransformation(Consumer<HttpRequestWrapper> consumer);
	}

	public interface ResponseCondition
	{
		ResponseConditionConfigurator anyResponse();

		ResponseConditionConfigurator responseHasHeader(String header);

		ResponseConditionConfigurator responseHasNoHeader(String header);

		ResponseConditionConfigurator responseHasHeaderWithValue(String header, String value);

		ResponseConditionConfigurator hasStatus(int statusCode);
	}

	public interface ResponseTransformation
	{
		ResponseTransformationConfigurator appendHtmlElementToBody(String tagName, Supplier<String> htmlContentSupplier);

		ResponseTransformationConfigurator removeHeaderFromResponse(String name);

		ResponseTransformationConfigurator setResponseHeader(String name, String value);

		ResponseTransformationConfigurator setResponseHeader(String name, Function<HttpResponseWrapper, String> headerValueEvaluator);

		ResponseTransformationConfigurator customResponseTransformation(Consumer<HttpResponseWrapper> consumer);

		ResponseTransformationConfigurator changeHtmlElement(String tagName, Predicate<Element> filter, Consumer<Element> action);

		default ResponseTransformationConfigurator changeHtmlElement(String tagName, Consumer<Element> action)
		{
			return changeHtmlElement(tagName, elem -> true, action);
		}

		ResponseTransformationConfigurator changeAllHtmlElements(String tagName, Predicate<Element> filter, Consumer<Element> action);

		default ResponseTransformationConfigurator changeAllHtmlElements(String tagName, Consumer<Element> action)
		{
			return changeAllHtmlElements(tagName, elem -> true, action);
		}
	}

	@AllArgsConstructor
	@lombok.Builder
	@Data
	public static class WebSurferRule
	{
		private String name;
		private Predicate<HttpRequestWrapper> requestPredicate;
		private Consumer<HttpRequestWrapper> requestTransformation;
		private Predicate<HttpResponseWrapper> responsePredicate;
		private Consumer<HttpResponseWrapper> responseTransformation;
	}

	@Data
	@NoArgsConstructor
	@EqualsAndHashCode(callSuper = true)
	public static class HttpResponseWrapper extends HttpWrapper
	{
		private JsonNode jsonBody;
		private Document domBody;
		private HttpStatus httpStatus;
		private ResponseEntity<String> originalResponse;
		private HttpRequestWrapper originalRequest;

		@lombok.Builder
		public HttpResponseWrapper(final HttpHeaders headers, final JsonNode jsonBody, final Document domBody, final HttpStatus httpStatus,
				final ResponseEntity<String> originalResponse, final HttpRequestWrapper originalRequest)
		{
			super(headers);
			this.jsonBody = jsonBody;
			this.domBody = domBody;
			this.httpStatus = httpStatus;
			this.originalResponse = originalResponse;
			this.originalRequest = originalRequest;
		}

		public ResponseEntity<Object> buildResponse()
		{

			final ResponseEntity.BodyBuilder response = ResponseEntity.status(httpStatus).headers(getHeaders());

			if(jsonBody != null)
			{
				return response.body(jsonBody);
			}

			else if (domBody != null)
			{
				return response.body(Parser.unescapeEntities(domBody.html(), true));
			}
			else
			{
				return response.body(originalResponse.getBody());
			}
		}
	}

	@Data
	@NoArgsConstructor
	@EqualsAndHashCode(callSuper = true)
	public static class HttpRequestWrapper extends HttpWrapper
	{
		private String method;
		private String url;
		private Object body;
		
		@lombok.Builder
		public HttpRequestWrapper(final HttpHeaders headers, final String method, final String url, final Object body)
		{
			super(headers);
			this.method = method;
			this.url = url;
			this.body = body;
		}
	}
	
	@AllArgsConstructor
	@NoArgsConstructor
	@Getter
	@Setter
	public static abstract class HttpWrapper
	{
		private HttpHeaders headers;
		
		public HttpWrapper addHeader(final String headerName, final List<String> headerValues)
		{
			getHeaders().put(headerName, headerValues);
			return this;
		}
		
		public HttpHeaders getHeaders()
		{
			if (headers == null)
			{
				headers = new HttpHeaders();
			}
			
			return headers;
		}
	}
}
