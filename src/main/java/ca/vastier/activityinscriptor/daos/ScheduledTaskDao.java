package ca.vastier.activityinscriptor.daos;

import ca.vastier.activityinscriptor.daos.ScheduledTaskEntity.TaskStatus;

import java.time.ZonedDateTime;
import java.util.Collection;

public interface ScheduledTaskDao
{
	/**
	 * .... Changes status to {@link TaskStatus#RUNNING}
	 * @param dateTime
	 * @return
	 */
	Collection<ScheduledTaskEntity> fetchTasksForExecution(ZonedDateTime dateTime);
	void changeTasksStatus(Collection<String> ids, TaskStatus status);
	void saveTask(ScheduledTaskEntity scheduledTaskEntity);
}
