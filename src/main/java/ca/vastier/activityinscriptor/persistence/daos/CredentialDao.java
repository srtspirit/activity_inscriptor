package ca.vastier.activityinscriptor.persistence.daos;

import ca.vastier.activityinscriptor.persistence.entities.CredentialEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;

@Repository
public interface CredentialDao extends MongoRepository<CredentialEntity, String>
{
	default CredentialEntity createCookieCredentialRecord(CredentialEntity entity)
	{
		return save(entity);
	}

	//TODO decouple from the spring jpa implementation
	Collection<CredentialEntity> findByCookieValue(String cookieValue);
	void removeByExpirationDateLessThan(LocalDateTime date);
}
