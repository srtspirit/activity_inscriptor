package ca.vastier.activityinscriptor.daos;

import ca.vastier.activityinscriptor.daos.ScheduledTaskEntity.TaskStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.ZonedDateTime;
import java.util.Collection;

public interface ScheduledTaskDao extends MongoRepository<ScheduledTaskEntity, String>, ScheduledTaskDaoCustom
{
	default void saveTask(ScheduledTaskEntity task) {save(task);}
}
