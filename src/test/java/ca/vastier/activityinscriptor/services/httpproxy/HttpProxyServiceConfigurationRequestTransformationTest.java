package ca.vastier.activityinscriptor.services.httpproxy;

import ca.vastier.activityinscriptor.services.httpproxy.HttpProxyService.HttpRequestWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HttpProxyServiceConfigurationRequestTransformationTest extends HttpProxyServiceConfiguratorImplTest
{
	@Test
	public void shouldSetRequestHeader()
	{
		when(httpRequestExecutor.executeRequest(any(HttpRequestWrapper.class))).thenReturn(defaultResponse);

		final HttpProxyService webSurfer = HttpProxyService.configure(httpRequestExecutor)
				.anyRequest()
				.setRequestHeader("requestHeader", "requestHeaderValue")
				.build();

		final HttpRequestWrapper request = HttpRequestWrapper.builder().build();
		webSurfer.surf(request);

		assertEquals("requestHeaderValue", request.getHeaders().getFirst("requestHeader"));
	}

	@Test
	public void shouldRemoveHeaderFromRequest()
	{
		when(httpRequestExecutor.executeRequest(any(HttpRequestWrapper.class))).thenReturn(defaultResponse);

		final HttpProxyService webSurfer = HttpProxyService.configure(httpRequestExecutor)
				.anyRequest()
				.removeHeaderFromRequest("forbiddenHeader")
				.build();

		final HttpHeaders headers = new HttpHeaders();
		headers.add("forbiddenHeader", "someValue");

		final HttpRequestWrapper request = HttpRequestWrapper.builder().headers(headers).build();
		webSurfer.surf(request);

		assertFalse(request.getHeaders().containsKey("forbiddenHeader"));
	}

	@Test
	public void shouldDoCustomRequestTransformation()
	{
		when(httpRequestExecutor.executeRequest(any(HttpRequestWrapper.class))).thenReturn(defaultResponse);

		final HttpProxyService webSurfer = HttpProxyService.configure(httpRequestExecutor)
				.anyRequest()
				.customRequestTransformation(mockRequestConsumer)
				.build();

		final HttpRequestWrapper request = HttpRequestWrapper.builder().build();
		webSurfer.surf(request);

		verify(mockRequestConsumer).accept(request);
	}

	@Test
	public void shouldChangeRequestUrl()
	{
		when(httpRequestExecutor.executeRequest(any(HttpRequestWrapper.class))).thenReturn(defaultResponse);

		final HttpProxyService webSurfer = HttpProxyService.configure(httpRequestExecutor).anyRequest().changeUrl("newUrl").build();

		final HttpRequestWrapper request = HttpRequestWrapper.builder().url("url").build();
		webSurfer.surf(request);

		assertEquals("newUrl", request.getUrl());
	}

	@Test
	public void shouldChangeRequestUrlWithEvaluator()
	{
		when(httpRequestExecutor.executeRequest(any(HttpRequestWrapper.class))).thenReturn(defaultResponse);

		final HttpProxyService webSurfer = HttpProxyService.configure(httpRequestExecutor)
				.anyRequest()
				.changeUrl(req -> "newUrl")
				.build();

		final HttpRequestWrapper request = HttpRequestWrapper.builder().url("url").build();
		webSurfer.surf(request);

		assertEquals("newUrl", request.getUrl());
	}

	@Test
	public void shouldNotThrowExceptionWhenRequestHeadersAreNull()
	{
		when(httpRequestExecutor.executeRequest(any(HttpRequestWrapper.class))).thenReturn(defaultResponse);

		final HttpProxyService webSurfer = HttpProxyService.configure(httpRequestExecutor)
				.anyRequest()
				.removeHeaderFromRequest("forbiddenHeader")
				.build();

		final HttpRequestWrapper request = HttpRequestWrapper.builder().headers(null).build();

		assertDoesNotThrow(() -> webSurfer.surf(request));
		assertEquals(0, request.getHeaders().size());
	}

	@Test
	public void shouldSetHeadersInRequestsWithNullHeaders()
	{
		when(httpRequestExecutor.executeRequest(any(HttpRequestWrapper.class))).thenReturn(defaultResponse);
		final HttpProxyService webSurfer = HttpProxyService.configure(httpRequestExecutor)
				.anyRequest()
				.setRequestHeader("requestHeader", "requestHeaderValue")
				.build();

		final HttpRequestWrapper request = HttpRequestWrapper.builder().headers(null).build();

		webSurfer.surf(request);
		assertEquals("requestHeaderValue", request.getHeaders().getFirst("requestHeader"));
	}

	@Test
	public void shouldThrowExceptionWhenConfiguredWithNullValues_requestTransformations()
	{
		assertThrows(NullPointerException.class, () -> HttpProxyService.configure(httpRequestExecutor)
				.anyRequest()
				.setRequestHeader(null, "requestHeaderValue")
				.build());
		assertThrows(NullPointerException.class,
				() -> HttpProxyService.configure(httpRequestExecutor).anyRequest().setRequestHeader("requestHeader", null).build());
		assertThrows(NullPointerException.class,
				() -> HttpProxyService.configure(httpRequestExecutor).anyRequest().removeHeaderFromRequest(null).build());
		assertThrows(NullPointerException.class,
				() -> HttpProxyService.configure(httpRequestExecutor).anyRequest().customRequestTransformation(null).build());
	}
}
