package ca.vastier.activityinscriptor.services.httpproxy;

import ca.vastier.activityinscriptor.services.httpproxy.HttpProxyService.HttpRequestWrapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HttpProxyServiceConfiguratorRequestPredicateTest extends HttpProxyServiceConfiguratorImplTest
{
	@Test
	public void shouldCheckGetMethod()
	{
		final HttpProxyService proxyService = HttpProxyService.configure(httpRequestExecutor)
				.get()
				.customRequestTransformation(mockRequestConsumer)

				.build();

		final HttpRequestWrapper request = HttpRequestWrapper.builder().build();

		request.setMethod("someMethod");
		proxyService.surf(request);
		verify(mockRequestConsumer, never()).accept(request);

		request.setMethod(null);
		proxyService.surf(request);
		verify(mockRequestConsumer, never()).accept(request);

		request.setMethod("GET");
		proxyService.surf(request);
		verify(mockRequestConsumer).accept(request);
	}

	@Test
	public void shouldCheckGetMethodWithUrl()
	{
		final HttpProxyService proxyService = HttpProxyService.configure(httpRequestExecutor)
				.get("url")
				.customRequestTransformation(mockRequestConsumer)

				.build();
		final HttpRequestWrapper request = HttpRequestWrapper.builder().build();

		request.setMethod("GET");
		proxyService.surf(request);
		verify(mockRequestConsumer, never()).accept(request);

		request.setUrl("someUrl");
		proxyService.surf(request);
		verify(mockRequestConsumer, never()).accept(request);

		request.setUrl("url");
		proxyService.surf(request);
		verify(mockRequestConsumer).accept(request);
	}

	@Test
	public void shouldCheckPostMethod()
	{
		final HttpProxyService proxyService = HttpProxyService.configure(httpRequestExecutor)
				.post()
				.customRequestTransformation(mockRequestConsumer)

				.build();
		final HttpRequestWrapper request = HttpRequestWrapper.builder().build();

		request.setMethod("someMethod");
		proxyService.surf(request);
		verify(mockRequestConsumer, never()).accept(request);

		request.setMethod(null);
		proxyService.surf(request);
		verify(mockRequestConsumer, never()).accept(request);

		request.setMethod("POST");
		proxyService.surf(request);
		verify(mockRequestConsumer).accept(request);
	}

	@Test
	public void shouldCheckPostMethodWithUrl()
	{
		final HttpProxyService proxyService = HttpProxyService.configure(httpRequestExecutor)
				.post("url")
				.customRequestTransformation(mockRequestConsumer)

				.build();
		final HttpRequestWrapper request = HttpRequestWrapper.builder().build();

		request.setMethod("POST");
		proxyService.surf(request);
		verify(mockRequestConsumer, never()).accept(request);

		request.setUrl("someUrl");
		proxyService.surf(request);
		verify(mockRequestConsumer, never()).accept(request);

		request.setUrl("url");
		proxyService.surf(request);
		verify(mockRequestConsumer).accept(request);
	}

	@Test
	public void shouldCheckPutMethod()
	{
		final HttpProxyService proxyService = HttpProxyService.configure(httpRequestExecutor)
				.put()
				.customRequestTransformation(mockRequestConsumer)
				.build();
		final HttpRequestWrapper request = HttpRequestWrapper.builder().build();

		request.setMethod("someMethod");
		proxyService.surf(request);
		verify(mockRequestConsumer, never()).accept(request);

		request.setMethod(null);
		proxyService.surf(request);
		verify(mockRequestConsumer, never()).accept(request);

		request.setMethod("PUT");
		proxyService.surf(request);
		verify(mockRequestConsumer).accept(request);
	}

	@Test
	public void shouldCheckPutMethodWithUrl()
	{
		final HttpProxyService proxyService = HttpProxyService.configure(httpRequestExecutor)
				.put("url")
				.customRequestTransformation(mockRequestConsumer)
				.build();
		final HttpRequestWrapper request = HttpRequestWrapper.builder().build();

		request.setMethod("PUT");
		proxyService.surf(request);
		verify(mockRequestConsumer, never()).accept(request);

		request.setUrl("someUrl");
		proxyService.surf(request);
		verify(mockRequestConsumer, never()).accept(request);

		request.setUrl("url");
		proxyService.surf(request);
		verify(mockRequestConsumer).accept(request);
	}

	@Test
	public void shouldCheckDeleteMethod()
	{
		final HttpProxyService proxyService = HttpProxyService.configure(httpRequestExecutor)
				.delete()
				.customRequestTransformation(mockRequestConsumer)
				.build();
		final HttpRequestWrapper request = HttpRequestWrapper.builder().build();

		request.setMethod("someMethod");
		proxyService.surf(request);
		verify(mockRequestConsumer, never()).accept(request);

		request.setMethod(null);
		proxyService.surf(request);
		verify(mockRequestConsumer, never()).accept(request);

		request.setMethod("DELETE");
		proxyService.surf(request);
		verify(mockRequestConsumer).accept(request);
	}

	@Test
	public void shouldCheckDeleteMethodWithUrl()
	{
		final HttpProxyService proxyService = HttpProxyService.configure(httpRequestExecutor)
				.delete("url")
				.customRequestTransformation(mockRequestConsumer)
				.build();
		final HttpRequestWrapper request = HttpRequestWrapper.builder().build();

		request.setMethod("DELETE");
		proxyService.surf(request);
		verify(mockRequestConsumer, never()).accept(request);

		request.setUrl("someUrl");
		proxyService.surf(request);
		verify(mockRequestConsumer, never()).accept(request);

		request.setUrl("url");
		proxyService.surf(request);
		verify(mockRequestConsumer).accept(request);
	}

	@Test
	public void shouldCheckRequestHeader()
	{
		final HttpProxyService proxyService = HttpProxyService.configure(httpRequestExecutor)
				.requestHasHeader("header")
				.customRequestTransformation(mockRequestConsumer)
				.build();
		final HttpRequestWrapper request = HttpRequestWrapper.builder().build();

		request.setHeaders(null);
		proxyService.surf(request);
		verify(mockRequestConsumer, never()).accept(request);

		request.addHeader("someHeader", List.of("value"));
		proxyService.surf(request);
		verify(mockRequestConsumer, never()).accept(request);

		request.addHeader("header", List.of("value"));
		proxyService.surf(request);
		verify(mockRequestConsumer).accept(request);

		reset(mockRequestConsumer);

		request.addHeader("header", null);
		proxyService.surf(request);
		verify(mockRequestConsumer).accept(request);
	}

	@Test
	public void shouldCheckRequestNoHeader()
	{
		final HttpProxyService proxyService = HttpProxyService.configure(httpRequestExecutor)
				.requestHasNoHeader("forbidden")
				.customRequestTransformation(mockRequestConsumer)
				.build();
		final HttpRequestWrapper request = HttpRequestWrapper.builder().build();

		request.addHeader("forbidden", List.of("value"));
		proxyService.surf(request);
		verify(mockRequestConsumer, never()).accept(request);

		request.setHeaders(null);
		proxyService.surf(request);
		verify(mockRequestConsumer).accept(request);

		reset(mockRequestConsumer);

		request.addHeader("someHeader", List.of("value"));
		proxyService.surf(request);
		verify(mockRequestConsumer).accept(request);
	}

	@Test
	public void shouldCheckRequestHeaderValue()
	{
		final HttpProxyService proxyService = HttpProxyService.configure(httpRequestExecutor)
				.requestHasHeaderWithValue("header", "value")
				.customRequestTransformation(mockRequestConsumer)
				.build();
		final HttpRequestWrapper request = HttpRequestWrapper.builder().build();

		request.setHeaders(null);
		proxyService.surf(request);
		verify(mockRequestConsumer, never()).accept(request);

		request.addHeader("someHeader", List.of("value"));
		proxyService.surf(request);
		verify(mockRequestConsumer, never()).accept(request);

		request.addHeader("header", List.of("something"));
		proxyService.surf(request);
		verify(mockRequestConsumer, never()).accept(request);

		request.addHeader("header", null);
		proxyService.surf(request);
		verify(mockRequestConsumer, never()).accept(request);

		request.getHeaders().set("header", "value");
		proxyService.surf(request);
		verify(mockRequestConsumer).accept(request);
	}

	@Test
	public void shouldCheckAnyRequest()
	{
		final HttpProxyService proxyService = HttpProxyService.configure(httpRequestExecutor)
				.anyRequest()
				.customRequestTransformation(mockRequestConsumer)
				.build();
		final HttpRequestWrapper request = HttpRequestWrapper.builder().build();

		proxyService.surf(request);
		verify(mockRequestConsumer).accept(request);
	}

	@Test
	public void shouldCheckMultipleRequests()
	{
		when(httpRequestExecutor.executeRequest(any(HttpRequestWrapper.class))).thenReturn(defaultResponse);

		//given
		//@formatter:off
		final HttpProxyService httpProxyService = HttpProxyService.configure(httpRequestExecutor)

				.get("getUrl")
				.requestHasHeader("header")
					.customRequestTransformation(mockRequestConsumer)

				.get("getUrl")
				.requestHasNoHeader("header")
					.customRequestTransformation(mockRequestConsumer)
					
				.anyRequest()
					.customRequestTransformation(mockRequestConsumer)

				.build();
		//@formatter:on

		final HttpRequestWrapper request = HttpRequestWrapper.builder().build();
		//when
		request.setMethod("GET");
		request.setUrl("getUrl");
		request.setHeaders(null);

		httpProxyService.surf(request);

		//then verify transformation invocations triggered by: get("getUrl") & hasNoHeader | any
		verify(mockRequestConsumer, times(2)).accept(request);

		reset(mockRequestConsumer);
		//when
		request.addHeader("header", List.of("value"));
		httpProxyService.surf(request);

		//then verify transformation invocations triggered by: get("gettUrl") & hasHeader | any
		verify(mockRequestConsumer, times(2)).accept(request);
	}

	@Test
	public void shouldThrowExceptionWhenConfiguredWithNullValues_requestPredicate()
	{
		assertThrows(NullPointerException.class, () -> HttpProxyService.configure(httpRequestExecutor)
				.get(null)
				.customRequestTransformation(mockRequestConsumer)
				.build());

		assertThrows(NullPointerException.class, () -> HttpProxyService.configure(httpRequestExecutor)
				.post(null)
				.customRequestTransformation(mockRequestConsumer)
				.build());

		assertThrows(NullPointerException.class, () -> HttpProxyService.configure(httpRequestExecutor)
				.put(null)
				.customRequestTransformation(mockRequestConsumer)
				.build());

		assertThrows(NullPointerException.class, () -> HttpProxyService.configure(httpRequestExecutor)
				.delete(null)
				.customRequestTransformation(mockRequestConsumer)
				.build());

		assertThrows(NullPointerException.class, () -> HttpProxyService.configure(httpRequestExecutor)
				.requestHasHeader(null)
				.customRequestTransformation(mockRequestConsumer));

		assertThrows(NullPointerException.class, () -> HttpProxyService.configure(httpRequestExecutor)
				.requestHasNoHeader(null)
				.customRequestTransformation(mockRequestConsumer));

		assertThrows(NullPointerException.class, () -> HttpProxyService.configure(httpRequestExecutor)
				.requestHasHeaderWithValue(null, "requestHeaderValue")
				.customRequestTransformation(mockRequestConsumer)
				.build());

		assertThrows(NullPointerException.class, () -> HttpProxyService.configure(httpRequestExecutor)
				.requestHasHeaderWithValue("header", null)
				.customRequestTransformation(mockRequestConsumer)
				.build());
	}
}
