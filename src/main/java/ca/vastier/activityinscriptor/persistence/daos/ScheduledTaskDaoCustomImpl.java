package ca.vastier.activityinscriptor.persistence.daos;

import ca.vastier.activityinscriptor.config.Constants;
import ca.vastier.activityinscriptor.persistence.entities.ScheduledTaskEntity;
import ca.vastier.activityinscriptor.persistence.namespaces.ScheduledTaskEntityNamespace;
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
	private static final int TIME_ZONES_IN_THE_WORLD = 24;
	private static final int LOOK_AHEAD_TIME_HOURS = (int)(TIME_ZONES_IN_THE_WORLD + Constants.MAX_PREPARATION_TASK_TIME_MS/3600000);

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
		final Query query = new Query().addCriteria(Criteria.where(ScheduledTaskEntityNamespace.Columns.START_TIME)
				.lte(searchingDateTime.plusHours(
						LOOK_AHEAD_TIME_HOURS)) //no time zones are kept in start time so consider all tasks within 24h plus warming up time
				.and(ScheduledTaskEntityNamespace.Columns.STATUS)
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
