package ca.vastier.activityinscriptor.persistence.entities;

import ca.vastier.activityinscriptor.persistence.namespaces.CredentialEntityNamespace;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = CredentialEntityNamespace.TABLE_NAME)
public class CredentialEntity
{
	@Field(name = CredentialEntityNamespace.Columns.COOKIE_VALUE)
	private String cookieValue;
	@Field(name = CredentialEntityNamespace.Columns.LOGIN)
	private String login;
	@Field(name = CredentialEntityNamespace.Columns.PASSWORD)
	private String password;
	@Field(name = CredentialEntityNamespace.Columns.EXPIRATION_DATE)
	private LocalDateTime expirationDate;
}
