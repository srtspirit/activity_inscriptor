package ca.vastier.activityinscriptor.services;

import ca.vastier.activityinscriptor.services.strategies.NormalFlowStrategy;
import ca.vastier.activityinscriptor.services.strategies.Strategy;
import ca.vastier.activityinscriptor.services.strategies.StubStrategy;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class WebSurfer2
{
	protected final Strategy strategy;

	public WebSurfer2(final Strategy strategy)
	{
		this.strategy = strategy;
	}

	public HttpResponseWrapper surf(final HttpRequestWrapper requestWrapper, final RestTemplate restTemplate)
	{
		if (strategy == null)
		{
			//TODO create exception
			throw new RuntimeException("Surfer has not been configured");
		}
		HttpResponseWrapper responseWrapper = strategy.surf(requestWrapper, restTemplate);

		return responseWrapper;
	}

	@Data
	@AllArgsConstructor
	@lombok.Builder
	public static class HttpResponseWrapper
	{
		Optional<JsonNode> jsonBody;
		Optional<Document> domBody;
		HttpHeaders headers;
		HttpStatus httpStatus;
		ResponseEntity<String> originalResponse;
	}

	@Getter
	@Setter
	public static class HttpRequestWrapper
	{
		String method;
		String url;
		Object body;
		HttpHeaders headers;
	}

	public static WebSurferConfigurator configure()
	{
		return  new WebSurferConfiguratorImpl();
	}

	public interface WebSurferConfigurator extends RequestCondition, ResponseCondition, Builder
	{}

	public interface RequestConditionConfigurator
			extends RequestCondition, RequestTransformation, ResponseCondition, ResponseTransformation
	{}

	public interface RequestTransformationConfigurator extends RequestCondition, RequestTransformation, ResponseCondition, ResponseTransformation, Builder
	{}

	public interface ResponseConditionConfigurator extends ResponseCondition, ResponseTransformation
	{}

	public interface ResponseTransformationConfigurator extends RequestCondition, ResponseCondition, ResponseTransformation, Builder{}

	public interface Builder
	{
		WebSurfer2 build();
	}

	public interface RequestCondition
	{
		RequestConditionConfigurator get(String url);
		RequestConditionConfigurator post(String url);
		RequestConditionConfigurator requestHasHeader(String header);
		RequestConditionConfigurator requestHasHeaderWithValue(String header, String value);
		RequestConditionConfigurator anyRequest();
	}

	public interface RequestTransformation
	{
		RequestTransformationConfigurator setRequestHeader(String name, String value);
		RequestTransformationConfigurator removeHeaderFromRequest(String name);
		WebSurferConfigurator stopProcessingReturnValue(HttpResponseWrapper responseWrapper);
	}

	public interface ResponseCondition
	{
		ResponseConditionConfigurator anyResponse();
		ResponseConditionConfigurator responseHasHeader(String header);
		ResponseConditionConfigurator responseHasHeaderWithValue(String header, String value);
		ResponseConditionConfigurator hasStatus(int statusCode);
	}

	public interface ResponseTransformation
	{
		ResponseTransformationConfigurator removeAllHyperLinks();
		ResponseTransformationConfigurator appendHtmlElementToBody(String htmlTemplateFileName, String stylesFileName);
		ResponseTransformationConfigurator removeHeaderFromResponse(String name);
		ResponseTransformationConfigurator changeBaseHrefInDomResponse(String newBaseHref);
		ResponseTransformationConfigurator setResponseHeader(String name, String value);
		ResponseTransformationConfigurator customTransformation(Consumer<HttpResponseWrapper> consumer);
	}

	public static class WebSurferConfiguratorImpl implements WebSurferConfigurator, RequestConditionConfigurator, RequestTransformationConfigurator, ResponseConditionConfigurator, ResponseTransformationConfigurator
	{
		Strategy strategy;

		Predicate<HttpRequestWrapper> requestPredicate = req -> true;
		Consumer<HttpRequestWrapper> requestTransformations;

		Predicate<HttpResponseWrapper> responsePredicate;
		Consumer<HttpResponseWrapper> responseTransformations;

		private void flush()
		{
			if (requestTransformations != null || responseTransformations != null)
			{
				final Strategy newStrategy = new NormalFlowStrategy(requestPredicate, requestTransformations, responsePredicate, responseTransformations);
				strategy = strategy == null? newStrategy: strategy.andThen(newStrategy);
			}

			requestPredicate = req -> true;
			requestTransformations = null;
			responsePredicate = null;
			responseTransformations = null;
		}

		@Override
		public RequestConditionConfigurator get(final String url)
		{
			flush();
			//TODO make use of URL wildcards
			Predicate<HttpRequestWrapper> method = requestWrapper -> requestWrapper.method.equals("GET");
			Predicate<HttpRequestWrapper> pred = method.and(requestWrapper -> url.equals(requestWrapper.url));
			requestPredicate = requestPredicate == null? pred: requestPredicate.and(pred);

			return this;
		}

		@Override
		public RequestConditionConfigurator post(final String url)
		{
			flush();
			Predicate<HttpRequestWrapper> method = requestWrapper -> requestWrapper.method.equals("POST");
			Predicate<HttpRequestWrapper> pred = method.and(requestWrapper -> url.equals(requestWrapper.url));
			requestPredicate = requestPredicate == null? pred: requestPredicate.and(pred);

			return this;
		}

		@Override
		public RequestConditionConfigurator requestHasHeader(final String header)
		{
			flush();
			Predicate<HttpRequestWrapper> head = requestWrapper -> requestWrapper.headers.containsKey(header);
			requestPredicate = requestPredicate == null? head: requestPredicate.and(head);

			return this;
		}

		@Override
		public RequestConditionConfigurator requestHasHeaderWithValue(final String header, final String value)
		{
			flush();
			Predicate<HttpRequestWrapper> head = requestWrapper -> requestWrapper.headers.get(header).stream().anyMatch(val -> val.equals(value));
			requestPredicate = requestPredicate == null? head: requestPredicate.and(head);

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
		public RequestTransformationConfigurator setRequestHeader(String name, String value)
		{
			Consumer<HttpRequestWrapper> cons = requestWrapper -> requestWrapper.headers.add(name, value);

			requestTransformations = requestTransformations == null? cons: requestTransformations.andThen(cons);

			return this;
		}

		@Override
		public RequestTransformationConfigurator removeHeaderFromRequest(final String name)
		{
			Consumer<HttpRequestWrapper> cons = requestWrapper -> requestWrapper.headers.remove(name);

			requestTransformations = requestTransformations == null? cons: requestTransformations.andThen(cons);

			return this;
		}

		@Override
		public ResponseConditionConfigurator anyResponse()
		{
			flush();
			Predicate<HttpResponseWrapper> pred = res -> true;
			responsePredicate = pred;

			return this;
		}

		@Override
		public ResponseConditionConfigurator responseHasHeader(final String header)
		{
			flush();
			Predicate<HttpResponseWrapper> head = responseWrapper -> responseWrapper.headers.containsKey(header);
			responsePredicate = responsePredicate == null? head: responsePredicate.and(head);

			return this;
		}

		@Override
		public ResponseConditionConfigurator responseHasHeaderWithValue(final String header, final String value)
		{
			flush();
			Predicate<HttpResponseWrapper> head = responseWrapper -> responseWrapper.headers.get(header).stream().anyMatch(val -> val.equals(value));
			responsePredicate = responsePredicate == null? head: responsePredicate.and(head);

			return this;
		}

		@Override
		public ResponseConditionConfigurator hasStatus(final int statusCode)
		{
			flush();
			Predicate<HttpResponseWrapper> status = responseWrapper -> responseWrapper.httpStatus.value() == statusCode;
			responsePredicate = responsePredicate == null? status: responsePredicate.and(status);

			return this;
		}

		@Override
		public ResponseTransformationConfigurator removeAllHyperLinks()
		{
			final Consumer<HttpResponseWrapper> consumer = responseWrapper -> {
				responseWrapper.domBody.ifPresent(body -> {
					body.select("a").forEach(a -> a.removeAttr("href"));
					body.select("button").stream()
							.filter(button -> button.attr("onclick").contains("document.location")).
							forEach(Node::remove);
				});
			};

			responseTransformations = responseTransformations == null? consumer: responseTransformations.andThen(consumer);

			return this;
		}

		@Override
		public ResponseTransformationConfigurator appendHtmlElementToBody(final String htmlTemplateFileName, final String stylesFileName)
		{
			final FileReader fileReader = new FileReader();
			final Consumer<HttpResponseWrapper> consumer = responseWrapper -> {
				responseWrapper.domBody.ifPresent(body -> {
					final String templateFileName = htmlTemplateFileName;
					final String styleFileName = stylesFileName;

					try
					{
						final String templateContent = fileReader.readFileAsString(templateFileName);
						final Element div = body.createElement("div");
						div.html(templateContent);
						body.body()
								.appendChild(div.child(
										0)); //use children and wrapper in order to keep the entire html in the file. Otherwise I would have to set up classes manually in java

						final String styleContent = fileReader.readFileAsString(styleFileName);
						final Element style = body.createElement("style");
						style.html(styleContent);
						body.body().appendChild(style);
					}
					catch (IOException e)
					{
						//TODO declare a good bean injection
						//LOGGER.warn("Could not read file {}: {}\n Impossible to add a disclaimer", templateFileName, e.getMessage());
					}
				});
			};

			responseTransformations = responseTransformations == null? consumer: responseTransformations.andThen(consumer);

			return this;
		}

		@Override
		public ResponseTransformationConfigurator removeHeaderFromResponse(final String name)
		{
			Consumer<HttpResponseWrapper> cons = responseWrapper -> responseWrapper.headers.remove(name);

			responseTransformations = responseTransformations == null? cons: responseTransformations.andThen(cons);

			return this;
		}

		@Override
		public ResponseTransformationConfigurator changeBaseHrefInDomResponse(final String newBaseHref)
		{
			final FileReader fileReader = new FileReader();
			final Consumer<HttpResponseWrapper> consumer = responseWrapper -> {
				responseWrapper.domBody.ifPresent(body -> {
					final String fileName = "templates/change-base-href-after-page-is-loaded.js";

					try
					{
						final String scriptText = fileReader.readFileAsString(fileName);
						final Element script = body.createElement("script");
						script.html(String.format(scriptText, "\"" + newBaseHref + "\""));
						body.body().appendChild(script);
					}
					catch (IOException e)
					{
						//TODO
					}
				});
			};

			responseTransformations = responseTransformations == null? consumer: responseTransformations.andThen(consumer);

			return this;
		}

		@Override
		public ResponseTransformationConfigurator setResponseHeader(final String name, final String value)
		{
			Consumer<HttpResponseWrapper> cons = responseWrapper -> responseWrapper.headers.add(name, value);

			responseTransformations = responseTransformations == null? cons: responseTransformations.andThen(cons);

			return this;
		}

		@Override
		public ResponseTransformationConfigurator customTransformation(final Consumer<HttpResponseWrapper> consumer)
		{
			responseTransformations = responseTransformations == null? consumer: responseTransformations.andThen(consumer);

			return this;
		}

		@Override
		public WebSurferConfigurator stopProcessingReturnValue(final HttpResponseWrapper responseWrapper)
		{
			strategy.andThen(new StubStrategy(requestPredicate, responseWrapper));
			requestPredicate = null;
			requestTransformations = null;

			return this;
		}

		public WebSurfer2 build()
		{
			return new WebSurfer2(strategy);
		}
	}
}
