package ca.vastier.activityinscriptor.services.httpproxy;

import ca.vastier.activityinscriptor.services.httpproxy.HttpProxyService.HttpRequestWrapper;
import ca.vastier.activityinscriptor.services.httpproxy.HttpProxyService.HttpResponseWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HttpProxyServiceConfiguratorResponsePredicateTest extends HttpProxyServiceConfiguratorImplTest
{
	@Test
	public void shouldCheckResponse()
	{
		//given
		//@formatter:off
		final HttpProxyService httpProxyService = HttpProxyService.configure(httpRequestExecutor)
				.anyResponse()
				.customResponseTransformation(mockResponseConsumer)

				.responseHasHeader("responseHeader")
				.customResponseTransformation(mockResponseConsumer)

				.responseHasNoHeader("forbiddenHeader")
				.customResponseTransformation(mockResponseConsumer)

				.responseHasHeaderWithValue("responseHeader", "responseHeaderValue")
				.customResponseTransformation(mockResponseConsumer)

				.hasStatus(200)
				.customResponseTransformation(mockResponseConsumer)

				.build();
		//@formatter:on

		HttpHeaders headers = new HttpHeaders();
		headers.add("responseHeader", "responseHeaderValue");

		final HttpResponseWrapper response = HttpResponseWrapper.builder().headers(headers).httpStatus(HttpStatus.OK).build();
		when(httpRequestExecutor.executeRequest(any(HttpRequestWrapper.class))).thenReturn(response);

		//when
		final HttpRequestWrapper request = HttpRequestWrapper.builder().method("GET").url("getUrl").build();
		httpProxyService.surf(request);

		//then verify transformation invocations triggered by: hasHeader, hasNoHeader, hasHeaderWithValue, any, status
		verify(mockResponseConsumer, times(5)).accept(any(HttpResponseWrapper.class));

		//when
		headers = new HttpHeaders();
		headers.add("forbiddenHeader", "someValue");
		reset(mockResponseConsumer);

		when(httpRequestExecutor.executeRequest(any(HttpRequestWrapper.class))).thenReturn(
				HttpResponseWrapper.builder().headers(headers).httpStatus(HttpStatus.FORBIDDEN).build());

		httpProxyService.surf(request);

		//then verify transformation invocations triggered by: any
		verify(mockResponseConsumer, times(1)).accept(any(HttpResponseWrapper.class));

		//when
		reset(mockResponseConsumer);
		headers = new HttpHeaders();
		headers.add("responseHeader", "someValue");
		when(httpRequestExecutor.executeRequest(any(HttpRequestWrapper.class))).thenReturn(
				HttpResponseWrapper.builder().headers(headers).httpStatus(HttpStatus.FORBIDDEN).build());

		httpProxyService.surf(request);

		//then verify transformation invocations triggered by: any, hasHeader, but NOT hasHeaderWithValue, hasNoHeader
		verify(mockResponseConsumer, times(3)).accept(any(HttpResponseWrapper.class));
	}

	@Test
	public void shouldCheckAnyResponse()
	{
		//given
		//@formatter:off
		final HttpProxyService httpProxyService = HttpProxyService.configure(httpRequestExecutor)
				.anyResponse()
					.customResponseTransformation(mockResponseConsumer)

				.build();
		//@formatter:on

		final HttpResponseWrapper response = HttpResponseWrapper.builder().httpStatus(HttpStatus.OK).build();
		when(httpRequestExecutor.executeRequest(any(HttpRequestWrapper.class))).thenReturn(response);

		//when
		final HttpRequestWrapper request = HttpRequestWrapper.builder().method("GET").url("getUrl").build();
		httpProxyService.surf(request);

		//then verify transformation invocations triggered by: hasHeader, hasNoHeader, hasHeaderWithValue, any, status
		verify(mockResponseConsumer).accept(any(HttpResponseWrapper.class));
	}

	@Test
	public void shouldCheckResponseHasHeader()
	{
		//given
		//@formatter:off
		final HttpProxyService httpProxyService = HttpProxyService.configure(httpRequestExecutor)
				.responseHasHeader("responseHeader")
					.customResponseTransformation(mockResponseConsumer)

				.build();
		//@formatter:on

		//when
		HttpHeaders headers = new HttpHeaders();
		headers.add("responseHeader", "responseHeaderValue");
		HttpResponseWrapper response = HttpResponseWrapper.builder().headers(headers).httpStatus(HttpStatus.OK).build();
		when(httpRequestExecutor.executeRequest(any(HttpRequestWrapper.class))).thenReturn(response);

		HttpRequestWrapper request = HttpRequestWrapper.builder().method("GET").url("getUrl").build();
		httpProxyService.surf(request);

		//then
		verify(mockResponseConsumer).accept(any(HttpResponseWrapper.class));

		reset(mockResponseConsumer);

		//when
		headers = null;
		response = HttpResponseWrapper.builder().headers(headers).httpStatus(HttpStatus.OK).build();
		when(httpRequestExecutor.executeRequest(any(HttpRequestWrapper.class))).thenReturn(response);

		request = HttpRequestWrapper.builder().method("GET").url("getUrl").build();
		httpProxyService.surf(request);

		//then
		verify(mockResponseConsumer, never()).accept(any(HttpResponseWrapper.class));
	}

	@Test
	public void shouldCheckResponseHasHeaderWithValue()
	{
		//TODO
	}

	@Test
	public void shouldCheckResponseHasNoHeader()
	{
		//TODO
	}

	@Test
	public void shouldCheckResponseHasStatus()
	{
		//TODO
	}

	@Test
	public void shouldCheckResponsesWithMultipleConditions()
	{
		//given
		//@formatter:off
		final HttpProxyService httpProxyService = HttpProxyService.configure(httpRequestExecutor)
				.hasStatus(200)
				.responseHasHeader("responseHeader")
				.customResponseTransformation(mockResponseConsumer)

				.build();
		//@formatter:on

		HttpHeaders headers = new HttpHeaders();
		headers.add("responseHeader", "someValue");

		//when
		when(httpRequestExecutor.executeRequest(any(HttpRequestWrapper.class))).thenReturn(
				HttpResponseWrapper.builder().headers(headers).httpStatus(HttpStatus.OK).build());

		final HttpRequestWrapper request = HttpRequestWrapper.builder().method("GET").url("getUrl").build();
		httpProxyService.surf(request);

		//then
		verify(mockResponseConsumer, times(1)).accept(any(HttpResponseWrapper.class));

		//when
		reset(mockResponseConsumer);
		when(httpRequestExecutor.executeRequest(any(HttpRequestWrapper.class))).thenReturn(
				HttpResponseWrapper.builder().headers(headers).httpStatus(HttpStatus.FORBIDDEN).build());

		httpProxyService.surf(request);

		//then
		verify(mockResponseConsumer, times(0)).accept(any(HttpResponseWrapper.class));

		//when
		headers = new HttpHeaders();
		headers.add("someHeader", "someHeaderValue");
		reset(mockResponseConsumer);
		when(httpRequestExecutor.executeRequest(any(HttpRequestWrapper.class))).thenReturn(
				HttpResponseWrapper.builder().headers(headers).httpStatus(HttpStatus.OK).build());

		httpProxyService.surf(request);

		//then
		verify(mockResponseConsumer, times(0)).accept(any(HttpResponseWrapper.class));
	}

	@Test
	public void shouldHandleResponseHeaderWithNullValue()
	{
		//given
		//@formatter:off
		final HttpProxyService httpProxyService = HttpProxyService.configure(httpRequestExecutor)
				.responseHasHeaderWithValue("responseHeader", "someValue")
				.customResponseTransformation(mockResponseConsumer)

				.build();
		//@formatter:on

		HttpHeaders headers = new HttpHeaders();
		headers.add("responseHeader", null);

		//when
		when(httpRequestExecutor.executeRequest(any(HttpRequestWrapper.class))).thenReturn(
				HttpResponseWrapper.builder().headers(headers).httpStatus(HttpStatus.OK).build());

		assertDoesNotThrow(() -> httpProxyService.surf(defaultRequest));

		//then
		verify(mockResponseConsumer, times(0)).accept(any(HttpResponseWrapper.class));
	}

	@Test
	public void shouldThrowExceptionWhenConfiguredWithNullValues_responsePredicate()
	{
		assertThrows(NullPointerException.class, () -> HttpProxyService.configure(httpRequestExecutor)
				.responseHasHeader(null)
				.customResponseTransformation(mockResponseConsumer)
				.build());

		assertThrows(NullPointerException.class, () -> HttpProxyService.configure(httpRequestExecutor)
				.responseHasHeaderWithValue(null, "someValue")
				.customResponseTransformation(mockResponseConsumer)
				.build());

		assertThrows(NullPointerException.class, () -> HttpProxyService.configure(httpRequestExecutor)
				.responseHasHeaderWithValue("someHeader", null)
				.customResponseTransformation(mockResponseConsumer)
				.build());

		assertThrows(NullPointerException.class, () -> HttpProxyService.configure(httpRequestExecutor)
				.responseHasNoHeader(null)
				.customResponseTransformation(mockResponseConsumer)
				.build());
	}

	@Test
	public void shouldUseRequestPredicateToTriggerResponseTransformations()
	{
		//given
		//@formatter:off
		final HttpProxyService httpProxyService = HttpProxyService.configure(httpRequestExecutor)
				.get()
				.customResponseTransformation(mockResponseConsumer)
				.build();
		//@formatter:on

		when(httpRequestExecutor.executeRequest(any(HttpRequestWrapper.class))).thenReturn(new HttpResponseWrapper());
		httpProxyService.surf(HttpRequestWrapper.builder().method("GET").build());

		verify(mockResponseConsumer).accept(any(HttpResponseWrapper.class));
	}

	@Test
	public void shouldUseBothPredicatesToTriggerResponseTransformations()
	{
		//given
		//@formatter:off
		final HttpProxyService httpProxyService = HttpProxyService.configure(httpRequestExecutor)
				.get()
				.hasStatus(200)
				.customResponseTransformation(mockResponseConsumer)
				.build();
		//@formatter:on

		when(httpRequestExecutor.executeRequest(any(HttpRequestWrapper.class))).thenReturn(
				HttpResponseWrapper.builder().httpStatus(HttpStatus.FORBIDDEN).build());

		final HttpRequestWrapper request = HttpRequestWrapper.builder().method("GET").build();
		//request.setUrl("getUrl");

		httpProxyService.surf(request);
		verify(mockResponseConsumer, times(0)).accept(any(HttpResponseWrapper.class));

		when(httpRequestExecutor.executeRequest(any(HttpRequestWrapper.class))).thenReturn(
				HttpResponseWrapper.builder().httpStatus(HttpStatus.OK).build());
		httpProxyService.surf(request);
		verify(mockResponseConsumer, times(1)).accept(any(HttpResponseWrapper.class));
	}
}
