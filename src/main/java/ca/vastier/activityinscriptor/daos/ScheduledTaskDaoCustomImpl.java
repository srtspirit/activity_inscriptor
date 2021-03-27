package ca.vastier.activityinscriptor.daos;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Repository
public class ScheduledTaskDaoCustomImpl implements ScheduledTaskDaoCustom
{
	private MongoTemplate mongoTemplate;

	@Autowired
	public ScheduledTaskDaoCustomImpl(final MongoTemplate mongoTemplate)
	{
		this.mongoTemplate = mongoTemplate;
	}

	@Override
	public Collection<ScheduledTaskEntity> fetchTasksForExecution(final ZonedDateTime dateTime)
	{
		ScheduledTaskEntity task = new ScheduledTaskEntity("id", null, null, Map.of("username", "vlasova.daria.89@gmail.com", "password", "1234-abcd", "domain", "longueuil", "eventId", "236505", "date", "2021-03-27", "visitors", "1", "delay", "1000"), ScheduledTaskEntity.TaskType.ACTIVITY_INSCRIPTOR, ScheduledTaskEntity.TaskStatus.SCHEDULED, "");
		return List.of(task);
	}

	@Override
	public void changeTasksStatus(final String id, final ScheduledTaskEntity.TaskStatus newStatus,
			final ScheduledTaskEntity.TaskStatus previousStatus)
	{

	}
}
