package ca.vastier.activityinscriptor.persistence.daos;

import ca.vastier.activityinscriptor.persistence.entities.ScheduledTaskEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;

public interface ScheduledTaskDao extends MongoRepository<ScheduledTaskEntity, String>, ScheduledTaskDaoCustom
{
	default ScheduledTaskEntity saveTask(ScheduledTaskEntity task)
	{
		return save(task);
	}

	default Collection<ScheduledTaskEntity> getAllTasks()
	{
		return findAll();
	}
}
