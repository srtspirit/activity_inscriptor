package ca.vastier.activityinscriptor.persistence.namespaces;

public class CredentialEntityNamespace
{
	private CredentialEntityNamespace(){}

	public static final String TABLE_NAME = "cookies";
	public static class Columns
	{
		public static final String COOKIE_VALUE = "cookieValue";
		public static final String LOGIN = "login";
		public static final String PASSWORD = "password";
		public static final String EXPIRATION_DATE = "expirationDate";
	}
}
