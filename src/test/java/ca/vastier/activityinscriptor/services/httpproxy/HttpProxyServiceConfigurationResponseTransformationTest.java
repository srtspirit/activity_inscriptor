package ca.vastier.activityinscriptor.services.httpproxy;

import ca.vastier.activityinscriptor.services.FileReader;
import ca.vastier.activityinscriptor.services.httpproxy.HttpProxyService.HttpResponseWrapper;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HttpProxyServiceConfigurationResponseTransformationTest extends HttpProxyServiceConfiguratorImplTest
{
	private final FileReader fileReader = new FileReader();

	@Test
	public void shouldAppendHtmlElementToBody()
	{
		final String innerHtmlOfElementToAppend = fileReader.readFileAsString("src/test/resources/http-proxy-test-files/html-element-to-append.html");
		//@formatter: off
		final HttpProxyService webSurfer = HttpProxyService.configure(httpRequestExecutor)
				.anyResponse()
				.appendHtmlElementToBody("myTag", () -> innerHtmlOfElementToAppend)

				.build();
		//@formatter: on

		final HttpResponseWrapper response = HttpResponseWrapper.builder()
				.domBody(Parser.parse(fileReader.readFileAsString("src/test/resources/http-proxy-test-files/html-response-example.html"),
						"http://localhost"))
				.build();
		when(httpRequestExecutor.executeRequest(defaultRequest)).thenReturn(response);

		final HttpResponseWrapper resultResponse = webSurfer.surf(defaultRequest);

		final List<Element> myTags = resultResponse.getDomBody().select("myTag");
		assertEquals(1, myTags.size());

		//because of formatting and prettifying problem the expected html string needs to be converted to Element and then be printed as a string.
		//Otherwise the content will be treat as different even though it is effectively identical
		assertEquals(Parser.parseBodyFragment(innerHtmlOfElementToAppend, "http://localhost").body().child(0).toString(),
				myTags.get(0).child(0).toString());
	}

	@Test
	public void shouldNotFailWhenAppendingHtmlElementToNullBody()
	{
		final String innerHtmlOfElementToAppend = fileReader.readFileAsString("src/test/resources/http-proxy-test-files/html-element-to-append.html");
		//@formatter: off
		final HttpProxyService webSurfer = HttpProxyService.configure(httpRequestExecutor)
				.anyResponse()
				.appendHtmlElementToBody("myTag", () -> innerHtmlOfElementToAppend)

				.build();
		//@formatter: on

		when(httpRequestExecutor.executeRequest(defaultRequest)).thenReturn(HttpResponseWrapper.builder().domBody(null).build());

		assertDoesNotThrow(() -> webSurfer.surf(defaultRequest));
	}

	@Test
	public void shouldNotFailOnAppendingMalformedHtmlElementToBody()
	{
		final String innerHtmlOfElementToAppend = fileReader.readFileAsString("src/test/resources/http-proxy-test-files/malformed-html-content.html");
		//@formatter: off
		final HttpProxyService webSurfer = HttpProxyService.configure(httpRequestExecutor)
				.anyResponse()
				.appendHtmlElementToBody("myTag", () -> innerHtmlOfElementToAppend)

				.build();
		//@formatter: on

		final HttpResponseWrapper response = HttpResponseWrapper.builder()
				.domBody(Parser.parse(fileReader.readFileAsString("src/test/resources/http-proxy-test-files/html-response-example.html"),
						"http://localhost"))
				.build();
		when(httpRequestExecutor.executeRequest(defaultRequest)).thenReturn(response);

		webSurfer.surf(defaultRequest);

		final List<Element> myTags = response.getDomBody().select("myTag");
		assertEquals(1, myTags.size());

		//because of formatting and prettifying problem the expected html string needs to be converted to Element and then be printed as a string.
		//Otherwise the content will be treat as different even though it is effectively identical
		assertEquals(Parser.parseBodyFragment(innerHtmlOfElementToAppend, "http://localhost").body().child(0).toString(),
				myTags.get(0).child(0).toString());
	}

	@Test
	public void shouldHandleExceptionThrownWhenAppendingHtmlElementToBody()
	{
		//@formatter: off
		final HttpProxyService webSurfer = HttpProxyService.configure(httpRequestExecutor)
				.anyResponse()
				.appendHtmlElementToBody("myTag", () -> {
					throw new RuntimeException();
				})

				.build();
		//@formatter: on

		when(httpRequestExecutor.executeRequest(defaultRequest)).thenReturn(HttpResponseWrapper.builder()
				.domBody(Parser.parse(fileReader.readFileAsString("src/test/resources/http-proxy-test-files/html-response-example.html"),
						"http://localhost"))
				.build());

		assertDoesNotThrow(() -> webSurfer.surf(defaultRequest));
	}

	@Test
	public void shouldSetHeadersInResponse()
	{
		final HttpProxyService webSurfer = HttpProxyService.configure(httpRequestExecutor)
				.anyResponse()
				.setResponseHeader("responseHeader", "responseHeaderValue")
				.build();

		when(httpRequestExecutor.executeRequest(defaultRequest)).thenReturn(defaultResponse);

		final HttpResponseWrapper resultResponse = webSurfer.surf(defaultRequest);

		assertEquals("responseHeaderValue", resultResponse.getHeaders().getFirst("responseHeader"));
	}

	@Test
	public void shouldSetHeadersInResponseSupplierHeaderValue()
	{
		final HttpProxyService webSurfer = HttpProxyService.configure(httpRequestExecutor)
				.anyResponse()
				.setResponseHeader("responseHeader", res -> "responseHeaderValue")
				.build();

		when(httpRequestExecutor.executeRequest(defaultRequest)).thenReturn(defaultResponse);

		final HttpResponseWrapper resultResponse = webSurfer.surf(defaultRequest);

		assertEquals("responseHeaderValue", resultResponse.getHeaders().getFirst("responseHeader"));
	}

	@Test
	public void shouldRemoveHeaderFromResponse()
	{
		final HttpProxyService webSurfer = HttpProxyService.configure(httpRequestExecutor)
				.anyResponse()
				.removeHeaderFromResponse("forbiddenHeader")
				.build();

		final HttpHeaders headers = new HttpHeaders();
		headers.put("forbiddenHeader", List.of("forbiddenHeader"));

		when(httpRequestExecutor.executeRequest(defaultRequest)).thenReturn(HttpResponseWrapper.builder().headers(headers).build());

		final HttpResponseWrapper resultResponse = webSurfer.surf(defaultRequest);

		assertFalse(resultResponse.getHeaders().containsKey("forbiddenHeader"));
	}

	@Test
	public void shouldNotFailWhenRemovingNotExistingHeaderFromResponse()
	{
		final HttpProxyService webSurfer = HttpProxyService.configure(httpRequestExecutor)
				.anyResponse()
				.removeHeaderFromResponse("forbiddenHeader")
				.build();

		when(httpRequestExecutor.executeRequest(defaultRequest)).thenReturn(
				HttpResponseWrapper.builder().headers(new HttpHeaders()).build());

		final HttpResponseWrapper resultResponse = webSurfer.surf(defaultRequest);

		assertFalse(resultResponse.getHeaders().containsKey("forbiddenHeader"));
	}

	@Test
	public void shouldNotFailWhenRemovingNullHeaderFromResponse()
	{
		final HttpProxyService webSurfer = HttpProxyService.configure(httpRequestExecutor)
				.anyResponse()
				.removeHeaderFromResponse("forbiddenHeader")
				.build();

		when(httpRequestExecutor.executeRequest(defaultRequest)).thenReturn(HttpResponseWrapper.builder().headers(null).build());

		assertDoesNotThrow(() -> webSurfer.surf(defaultRequest));
	}

	@Test
	public void shouldDoCustomResponseTransformation()
	{
		final HttpProxyService webSurfer = HttpProxyService.configure(httpRequestExecutor)
				.anyResponse()
				.customResponseTransformation(mockResponseConsumer)
				.build();

		when(httpRequestExecutor.executeRequest(defaultRequest)).thenReturn(defaultResponse);

		webSurfer.surf(defaultRequest);

		verify(mockResponseConsumer).accept(defaultResponse);
	}

	@Test
	public void shouldModifyUniqueHtmlElement()
	{
		//@formatter: off
		final HttpProxyService webSurfer = HttpProxyService.configure(httpRequestExecutor)
				.anyResponse()
				.changeHtmlElement("p", paragraph -> paragraph.attr("customAttr").equals("changeMeUniqueAttr"),
						paragraph -> paragraph.text("new text"))

				.build();
		//@formatter: on

		final HttpResponseWrapper response = HttpResponseWrapper.builder()
				.domBody(Parser.parse(fileReader.readFileAsString("src/test/resources/http-proxy-test-files/html-response-example.html"),
						"http://localhost"))
				.build();
		when(httpRequestExecutor.executeRequest(defaultRequest)).thenReturn(response);

		final HttpResponseWrapper resultResponse = webSurfer.surf(defaultRequest);

		final List<Element> modifiedElements = resultResponse.getDomBody()
				.select("p")
				.stream()
				.filter(paragraph -> paragraph.attr("customAttr").equals("changeMeUniqueAttr"))
				.collect(Collectors.toList());

		assertEquals(1, modifiedElements.size());
		assertEquals("new text", modifiedElements.get(0).text());
	}

	@Test
	public void shouldModifyOneNotUniqueHtmlElement()
	{
		//@formatter: off
		final HttpProxyService webSurfer = HttpProxyService.configure(httpRequestExecutor)
				.anyResponse()
				.changeHtmlElement("p", paragraph -> paragraph.attr("customAttr").equals("changeMe"),
						paragraph -> paragraph.text("new text"))

				.build();
		//@formatter: on

		final HttpResponseWrapper response = HttpResponseWrapper.builder()
				.domBody(Parser.parse(fileReader.readFileAsString("src/test/resources/http-proxy-test-files/html-response-example.html"),
						"http://localhost"))
				.build();
		when(httpRequestExecutor.executeRequest(defaultRequest)).thenReturn(response);

		final HttpResponseWrapper resultResponse = webSurfer.surf(defaultRequest);

		final List<Element> modifiedElements = resultResponse.getDomBody()
				.select("p")
				.stream()
				.filter(paragraph -> paragraph.attr("customAttr").equals("changeMe"))
				.collect(Collectors.toList());

		assertEquals(2, modifiedElements.size());
		assertEquals("new text", modifiedElements.get(0).text());
		assertNotEquals("new text", modifiedElements.get(1).text());
	}

	@Test
	public void shouldModifyOneHtmlElementNoFilter()
	{
		//@formatter: off
		final HttpProxyService webSurfer = HttpProxyService.configure(httpRequestExecutor)
				.anyResponse()
				.changeHtmlElement("p", paragraph -> paragraph.text("new text"))

				.build();
		//@formatter: on

		final HttpResponseWrapper response = HttpResponseWrapper.builder()
				.domBody(Parser.parse(fileReader.readFileAsString("src/test/resources/http-proxy-test-files/html-response-example.html"),
						"http://localhost"))
				.build();
		when(httpRequestExecutor.executeRequest(defaultRequest)).thenReturn(response);

		final HttpResponseWrapper resultResponse = webSurfer.surf(defaultRequest);

		final List<Element> modifiedElements = new ArrayList<>(resultResponse.getDomBody().select("p"));

		assertEquals(3, modifiedElements.size());
		assertEquals("new text", modifiedElements.get(0).text());
		assertNotEquals("new text", modifiedElements.get(1).text());
		assertNotEquals("new text", modifiedElements.get(2).text());
	}

	@Test
	public void shouldModifyAllHtmlElement()
	{
		//@formatter: off
		final HttpProxyService webSurfer = HttpProxyService.configure(httpRequestExecutor)
				.anyResponse()
				.changeAllHtmlElements("p", paragraph -> paragraph.attr("customAttr").equals("changeMe"),
						paragraph -> paragraph.text("new text"))

				.build();
		//@formatter: on

		final HttpResponseWrapper response = HttpResponseWrapper.builder()
				.domBody(Parser.parse(fileReader.readFileAsString("src/test/resources/http-proxy-test-files/html-response-example.html"),
						"http://localhost"))
				.build();
		when(httpRequestExecutor.executeRequest(defaultRequest)).thenReturn(response);

		final HttpResponseWrapper resultResponse = webSurfer.surf(defaultRequest);

		final List<Element> modifiedElements = resultResponse.getDomBody()
				.select("p")
				.stream()
				.filter(paragraph -> paragraph.attr("customAttr").equals("changeMe"))
				.collect(Collectors.toList());

		assertEquals(2, modifiedElements.size());
		assertEquals("new text", modifiedElements.get(0).text());
		assertEquals("new text", modifiedElements.get(1).text());
	}

	@Test
	public void shouldNotFailWhenModifyingNullBodyOneElement()
	{
		//@formatter: off
		final HttpProxyService webSurfer = HttpProxyService.configure(httpRequestExecutor)
				.anyResponse()
				.changeHtmlElement("myTag", myTag -> myTag.text("new text"))

				.build();
		//@formatter: on

		final HttpResponseWrapper response = HttpResponseWrapper.builder().domBody(null).build();
		when(httpRequestExecutor.executeRequest(defaultRequest)).thenReturn(response);

		assertDoesNotThrow(() -> webSurfer.surf(defaultRequest));
	}

	@Test
	public void shouldNotFailWhenModifyingUnexistingElementsOneElement()
	{
		//@formatter: off
		final HttpProxyService webSurfer = HttpProxyService.configure(httpRequestExecutor)
				.anyResponse()
				.changeHtmlElement("myTag", myTag -> myTag.text("new text"))

				.build();
		//@formatter: on

		final String htmlResponseExample = fileReader.readFileAsString("src/test/resources/http-proxy-test-files/html-response-example.html");
		final HttpResponseWrapper response = HttpResponseWrapper.builder()
				.domBody(Parser.parse(htmlResponseExample, "http://localhost"))
				.build();
		when(httpRequestExecutor.executeRequest(defaultRequest)).thenReturn(response);

		final HttpResponseWrapper resultResponse = webSurfer.surf(defaultRequest);

		assertEquals(Parser.parse(htmlResponseExample, "http://localhost").toString(), resultResponse.getDomBody().toString());
	}

	@Test
	public void shouldNotFailWhenModifyingNullBodyAllElements()
	{
		//@formatter: off
		final HttpProxyService webSurfer = HttpProxyService.configure(httpRequestExecutor)
				.anyResponse()
				.changeAllHtmlElements("myTag", myTag -> myTag.text("new text"))

				.build();
		//@formatter: on

		final HttpResponseWrapper response = HttpResponseWrapper.builder().domBody(null).build();
		when(httpRequestExecutor.executeRequest(defaultRequest)).thenReturn(response);

		assertDoesNotThrow(() -> webSurfer.surf(defaultRequest));
	}

	@Test
	public void shouldNotFailWhenModifyingUnexistingElementsAllelements()
	{
		//@formatter: off
		final HttpProxyService webSurfer = HttpProxyService.configure(httpRequestExecutor)
				.anyResponse()
				.changeAllHtmlElements("myTag", myTag -> myTag.text("new text"))

				.build();
		//@formatter: on

		final String htmlResponseExample = fileReader.readFileAsString("src/test/resources/http-proxy-test-files/html-response-example.html");
		final HttpResponseWrapper response = HttpResponseWrapper.builder()
				.domBody(Parser.parse(htmlResponseExample, "http://localhost"))
				.build();
		when(httpRequestExecutor.executeRequest(defaultRequest)).thenReturn(response);

		final HttpResponseWrapper resultResponse = webSurfer.surf(defaultRequest);

		assertEquals(Parser.parse(htmlResponseExample, "http://localhost").toString(), resultResponse.getDomBody().toString());
	}

	@Test
	public void shouldModifyAllHtmlElementNoFilter()
	{
		//@formatter: off
		final HttpProxyService webSurfer = HttpProxyService.configure(httpRequestExecutor)
				.anyResponse()
				.changeAllHtmlElements("p", paragraph -> paragraph.text("new text"))

				.build();
		//@formatter: on

		final HttpResponseWrapper response = HttpResponseWrapper.builder()
				.domBody(Parser.parse(fileReader.readFileAsString("src/test/resources/http-proxy-test-files/html-response-example.html"),
						"http://localhost"))
				.build();
		when(httpRequestExecutor.executeRequest(defaultRequest)).thenReturn(response);

		final HttpResponseWrapper resultResponse = webSurfer.surf(defaultRequest);

		final List<Element> modifiedElements = new ArrayList<>(resultResponse.getDomBody().select("p"));

		assertEquals(3, modifiedElements.size());
		assertEquals("new text", modifiedElements.get(0).text());
		assertEquals("new text", modifiedElements.get(1).text());
		assertEquals("new text", modifiedElements.get(2).text());
	}

	@Test
	public void shouldDoMultipleModifications()
	{
		//@formatter: off
		final HttpProxyService webSurfer = HttpProxyService.configure(httpRequestExecutor)
				.anyResponse()
				.setResponseHeader("responseHeader", "headerValue")
				.changeAllHtmlElements("p", paragraph -> paragraph.text("new text"))
				.customResponseTransformation(mockResponseConsumer)

				.build();
		//@formatter: on

		final HttpResponseWrapper response = HttpResponseWrapper.builder()
				.domBody(Parser.parse(fileReader.readFileAsString("src/test/resources/http-proxy-test-files/html-response-example.html"),
						"http://localhost"))
				.build();
		when(httpRequestExecutor.executeRequest(defaultRequest)).thenReturn(response);

		final HttpResponseWrapper resultResponse = webSurfer.surf(defaultRequest);

		final List<Element> modifiedElements = new ArrayList<>(resultResponse.getDomBody().select("p"));

		verify(mockResponseConsumer).accept(response);
		assertEquals("headerValue", resultResponse.getHeaders().getFirst("responseHeader"));
		assertEquals(3, modifiedElements.size());
		assertEquals("new text", modifiedElements.get(0).text());
		assertEquals("new text", modifiedElements.get(1).text());
		assertEquals("new text", modifiedElements.get(2).text());
	}
}
