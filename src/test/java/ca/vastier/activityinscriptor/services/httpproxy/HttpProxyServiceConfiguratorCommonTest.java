package ca.vastier.activityinscriptor.services.httpproxy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class HttpProxyServiceConfiguratorCommonTest extends HttpProxyServiceConfiguratorImplTest
{
	@Test
	public void shouldThrowExceptionWhenConfiguredWithNullValues_requestExecutor()
	{
		assertThrows(NullPointerException.class,
				() -> HttpProxyService.configure(null).get().customRequestTransformation(mockRequestConsumer).build());
	}
}
