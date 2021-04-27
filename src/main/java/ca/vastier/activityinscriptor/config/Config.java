package ca.vastier.activityinscriptor.config;

import ca.vastier.activityinscriptor.persistence.daos.CredentialDao;
import ca.vastier.activityinscriptor.services.FileReader;
import ca.vastier.activityinscriptor.services.WebSurfer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class Config
{
	@Value("${longueuil.fliipapp.com: ''}")
	public String longueuilFliipApp;

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
	public WebSurfer longueuilFliipAppSurfer(final RestTemplate restTemplate, final FileReader fileReader,
			final CredentialDao credentialDao, @Qualifier("utcClock") final Clock utcClock)
	{
		return new WebSurfer(restTemplate, fileReader, credentialDao, longueuilFliipApp, utcClock);
	}
}
