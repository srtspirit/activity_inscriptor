package ca.vastier.activityinscriptor.persistence.daos;

import ca.vastier.activityinscriptor.persistence.entities.ScheduledTaskEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.Optional;

public interface ScheduledTaskDao extends MongoRepository<ScheduledTaskEntity, String>, ScheduledTaskDaoCustom
{
	default ScheduledTaskEntity saveTask(ScheduledTaskEntity task)
	{
		return save(task);
	}

	default Optional<ScheduledTaskEntity> findTaskById(String id)
	{
		return findById(id);
	}

	default void deleteTaskById(String id)
	{
		deleteById(id);
	}

	default Collection<ScheduledTaskEntity> getAllTasks()
	{
		return findAll();
	}

	default boolean doesTaskWithIdExist(String id)
	{
		return existsById(id);
	}
}
