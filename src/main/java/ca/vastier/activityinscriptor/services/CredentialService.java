package ca.vastier.activityinscriptor.services;

import ca.vastier.activityinscriptor.persistence.daos.CredentialDao;
import ca.vastier.activityinscriptor.persistence.entities.CredentialEntity;
import ca.vlastier.httpproxy.HttpProxyService;
import org.springframework.stereotype.Component;

import java.net.HttpCookie;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;

@Component
public class CredentialService
{
	private final Clock utcClock;
	private final CredentialDao credentialDao;

	public CredentialService(final Clock utcClock, final CredentialDao credentialDao)
	{
		this.utcClock = utcClock;
		this.credentialDao = credentialDao;
	}

	public void saveCredentialsWithCookie(final HttpProxyService.HttpResponseWrapper responseWrapper)
	{
		//TODO what if format is not good
		final String[] bodyParams = responseWrapper.getOriginalRequest().getBody().toString().split("\\&");
		final String login = bodyParams[0].substring(bodyParams[0].indexOf("=") + 1);
		final String password = bodyParams[1].substring(bodyParams[1].indexOf("=") + 1);

		final String cookieHeader = responseWrapper.getHeaders().get("set-cookie")
				.stream()
				.filter(cookie -> cookie.startsWith("ci_session"))
				.findFirst()
				.orElse("");
		final HttpCookie ciSessionCookie = HttpCookie.parse(cookieHeader).get(0);
		final CredentialEntity entity = CredentialEntity.builder()
				.cookieValue(ciSessionCookie.getValue())
				.login(URLDecoder.decode(login, StandardCharsets.UTF_8))
				.password(URLDecoder.decode(password, StandardCharsets.UTF_8))
				.expirationDate(LocalDateTime.now(utcClock).plusSeconds(ciSessionCookie.getMaxAge()))
				.build();

		credentialDao.save(entity);
	}
}
