package ca.vastier.activityinscriptor.services.httpproxy;

import ca.vastier.activityinscriptor.services.httpproxy.HttpProxyService.HttpRequestWrapper;
import ca.vastier.activityinscriptor.services.httpproxy.HttpProxyService.HttpResponseWrapper;

public interface HttpRequestExecutor
{
	HttpResponseWrapper executeRequest(HttpRequestWrapper request);
}
