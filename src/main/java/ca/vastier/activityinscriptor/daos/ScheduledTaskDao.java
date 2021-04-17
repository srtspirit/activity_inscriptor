package ca.vastier.activityinscriptor.daos;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ScheduledTaskDao extends MongoRepository<ScheduledTaskEntity, String>, ScheduledTaskDaoCustom
{
	default ScheduledTaskEntity saveTask(ScheduledTaskEntity task)
	{
		return save(task);
	}
}
