package ca.vastier.activityinscriptor.services.httpproxy;

import ca.vastier.activityinscriptor.services.httpproxy.HttpProxyService.HttpRequestWrapper;
import ca.vastier.activityinscriptor.services.httpproxy.HttpProxyService.HttpResponseWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HttpProxyServiceTest
{
	private final List<HttpProxyService.WebSurferRule> rules = new ArrayList<>();
	private HttpProxyService service;

	private HttpProxyService.WebSurferRule rule1;
	private HttpProxyService.WebSurferRule rule2;
	private HttpProxyService.WebSurferRule rule3;

	@Mock
	private HttpRequestExecutor httpRequestExecutor;

	@Mock
	private Predicate<HttpRequestWrapper> requestPredicate1;
	@Mock
	private Consumer<HttpRequestWrapper> requestTransformation1;
	@Mock
	private Predicate<HttpResponseWrapper> responsePredicate1;
	@Mock
	private Consumer<HttpResponseWrapper> responseTransformation1;
	@Mock
	private Predicate<HttpRequestWrapper> requestPredicate2;
	@Mock
	private Consumer<HttpRequestWrapper> requestTransformation2;
	@Mock
	private Predicate<HttpResponseWrapper> responsePredicate2;
	@Mock
	private Consumer<HttpResponseWrapper> responseTransformation2;
	@Mock
	private Predicate<HttpRequestWrapper> requestPredicate3;
	@Mock
	private Consumer<HttpRequestWrapper> requestTransformation3;
	@Mock
	private Predicate<HttpResponseWrapper> responsePredicate3;
	@Mock
	private Consumer<HttpResponseWrapper> responseTransformation3;

	@BeforeEach
	public void setup()
	{
		rule1 = HttpProxyService.WebSurferRule.builder()
				.requestPredicate(requestPredicate1)
				.requestTransformation(requestTransformation1)
				.responsePredicate(responsePredicate1)
				.responseTransformation(responseTransformation1)
				.build();
		rule2 = HttpProxyService.WebSurferRule.builder()
				.requestPredicate(requestPredicate2)
				.requestTransformation(requestTransformation2)
				.responsePredicate(responsePredicate2)
				.responseTransformation(responseTransformation2)
				.build();
		rule3 = HttpProxyService.WebSurferRule.builder()
				.requestPredicate(requestPredicate3)
				.requestTransformation(requestTransformation3)
				.responsePredicate(responsePredicate3)
				.responseTransformation(responseTransformation3)
				.build();

		service = new HttpProxyService(httpRequestExecutor, rules);
	}

	@Test
	public void shouldDoBothTransformationsWhenBothPredicatesTrue()
	{
		final String url = "url";
		final Object body = new Object();
		final HttpHeaders headers = new HttpHeaders();
		when(httpRequestExecutor.executeRequest(any(HttpRequestWrapper.class))).thenReturn(new HttpResponseWrapper());

		final HttpRequestWrapper request = HttpRequestWrapper.builder()
				.method(HttpMethod.GET.name())
				.url(url)
				.body(body)
				.headers(headers)
				.build();
		rules.add(rule1);

		when(requestPredicate1.test(request)).thenReturn(true);
		when(responsePredicate1.test(any())).thenReturn(true);
		service.surf(request);

		verify(requestTransformation1).accept(request);
		verify(responseTransformation1).accept(any());
		verify(httpRequestExecutor).executeRequest(request);
	}

	@Test
	public void shouldDoRequestTransformationsWhenRequestPredicatesTrue()
	{
		final String url = "url";
		final Object body = new Object();
		final HttpHeaders headers = new HttpHeaders();
		when(httpRequestExecutor.executeRequest(any(HttpRequestWrapper.class))).thenReturn(new HttpResponseWrapper());

		final HttpRequestWrapper request = HttpRequestWrapper.builder()
				.method(HttpMethod.GET.name())
				.url(url)
				.body(body)
				.headers(headers)
				.build();

		rules.add(rule1);

		when(requestPredicate1.test(request)).thenReturn(true);
		when(responsePredicate1.test(any())).thenReturn(false);
		service.surf(request);

		verify(requestTransformation1).accept(request);
		verify(responseTransformation1, never()).accept(any());
		verify(httpRequestExecutor).executeRequest(request);
	}

	@Test
	public void shouldNotDoTransformationsWhenBothPredicatesFalse()
	{
		final String url = "url";
		final Object body = new Object();
		final HttpHeaders headers = new HttpHeaders();
		when(httpRequestExecutor.executeRequest(any(HttpRequestWrapper.class))).thenReturn(new HttpResponseWrapper());

		final HttpRequestWrapper request = HttpRequestWrapper.builder()
				.method(HttpMethod.GET.name())
				.url(url)
				.body(body)
				.headers(headers)
				.build();

		rules.add(rule1);

		when(requestPredicate1.test(request)).thenReturn(false);
		service.surf(request);

		verify(requestTransformation1, never()).accept(request);
		verify(responseTransformation1, never()).accept(any());
		verify(httpRequestExecutor).executeRequest(request);
	}

	@Test
	public void shouldThrowExceptionWhenCalledWithNulls()
	{
		assertThrows(NullPointerException.class, () -> service.surf(null));
	}
}
