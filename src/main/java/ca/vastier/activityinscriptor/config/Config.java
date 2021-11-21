package ca.vastier.activityinscriptor.config;

import ca.vastier.activityinscriptor.services.CredentialService;
import ca.vastier.activityinscriptor.services.FileReader;
import ca.vastier.activityinscriptor.services.httpproxy.HttpProxyService;
import ca.vastier.activityinscriptor.services.httpproxy.HttpRequestExecutor;
import org.jsoup.nodes.Node;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class Config
{
	@Value("${longueuil.fliipapp.com: ''}")
	public String longueuilFliipApp;
	@Value("${web.surf.application.address}")
	private String webSurfApplicationAddress;
	@Value("${longueuil.fliipapp.com}")
	private String longueuilApplicationAddress;

	@Bean
	public RestTemplate restTemplate()
	{
		return new RestTemplate();
	}

	@Bean("montrealClock")
	public Clock montrealClock()
	{
		return Clock.system(ZoneId.of("America/Montreal"));
	}

	@Bean("utcClock")
	public Clock utcClock()
	{
		return Clock.systemUTC();
	}

	@Bean
	public HttpProxyService httpProxyService(final FileReader fileReader, final HttpRequestExecutor httpRequestExecutor,
			final CredentialService credentialService)
	{
		//@formatter: off
		return HttpProxyService.configure(httpRequestExecutor)

				.ruleName("any request rule")
				.anyRequest()
				.removeHeaderFromRequest("accept-encoding")
				.setRequestHeader(HttpHeaders.ORIGIN, longueuilApplicationAddress)
				.setRequestHeader(HttpHeaders.REFERER, longueuilApplicationAddress)
				.setRequestHeader(HttpHeaders.HOST, longueuilApplicationAddress)
				.changeUrl(request -> request.getUrl().replaceFirst(webSurfApplicationAddress, longueuilApplicationAddress))
				.removeHeaderFromResponse("transfer-encoding")

				.ruleName("html pages rule")
				.responseHasHeaderWithValue("content-type", "text/html")
				.changeAllHtmlElements("a", anchor -> anchor.removeAttr("href"))
				.changeAllHtmlElements("button", button -> button.attr("onclick").contains("document.location"), Node::remove)
				.appendHtmlElementToBody("div", () -> fileReader.readFileAsString("templates/disclaimer.html"))
				.appendHtmlElementToBody("script",
						() -> fileReader.readFileAsString("templates/change-base-href-after-page-is-loaded.js",
								"\"" + webSurfApplicationAddress + "\""))

				.ruleName("forwarded request rule")
				.hasStatus(302)
				.responseHasHeader(HttpHeaders.LOCATION)
				.setResponseHeader(HttpHeaders.LOCATION, responseWrapper -> responseWrapper.getHeaders()
						.getFirst("location")
						.replaceFirst("https?:\\/\\/[^\\/]+\\/", webSurfApplicationAddress))

				.ruleName("spoof script on the dashboard")
				.get("\\/home\\/dashboard($|[\\/?])") // regex for **/home/dashboard
				.changeHtmlElement("script", script -> script.attr("src").contains("home__dashboard_user.js"),
						script -> script.attr("src", webSurfApplicationAddress + "file/js/home__dashboard_user.js"))

				.ruleName("save credential cookies rule")
				.post("\\/home\\/login($|[\\/?])") //regex for **/home/login
				.hasStatus(302)
				.customResponseTransformation(responseWrapper -> credentialService.saveCredentialsWithCookie(responseWrapper))

				.build();
		//@formatter: on
	}
}
