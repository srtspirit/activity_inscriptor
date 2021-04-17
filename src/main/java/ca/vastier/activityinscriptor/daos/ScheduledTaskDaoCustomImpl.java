package ca.vastier.activityinscriptor.daos;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.stream.Collectors;

@Repository
public class ScheduledTaskDaoCustomImpl implements ScheduledTaskDaoCustom
{
	private final MongoTemplate mongoTemplate;

	@Autowired
	public ScheduledTaskDaoCustomImpl(final MongoTemplate mongoTemplate)
	{
		this.mongoTemplate = mongoTemplate;
	}

	@Override
	public Collection<ScheduledTaskEntity> fetchTasksForExecution(final ZonedDateTime dateTime)
	{
		final LocalDateTime searchingDateTime = dateTime.toLocalDateTime();
		final Query query = new Query().addCriteria(Criteria.where("startTime")
				.lte(searchingDateTime.plusHours(
						25)) // 25 is 24h in a day + 1h of possible warmimg time. Looking to a day ahead because no time zone saved in the field startTime
				.and("status")
				.is(ScheduledTaskEntity.TaskStatus.SCHEDULED));

		final Collection<ScheduledTaskEntity> foundEntities = mongoTemplate.find(query, ScheduledTaskEntity.class);
		return foundEntities.stream()
				.filter(ent -> ent.getStartTime()
						.minus(ent.getRequiredPreparationTime(), ChronoUnit.MILLIS)
						.isBefore(searchingDateTime))
				.collect(Collectors.toList());
	}

	@Override
	public void changeTasksStatus(final String id, final ScheduledTaskEntity.TaskStatus newStatus,
			final ScheduledTaskEntity.TaskStatus previousStatus)
	{

	}
}
