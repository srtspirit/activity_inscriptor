package ca.vastier.activityinscriptor.persistence.daos;

import ca.vastier.activityinscriptor.persistence.entities.ScheduledTaskEntity;
import ca.vastier.activityinscriptor.persistence.entities.ScheduledTaskEntity.TaskStatus;

import java.time.ZonedDateTime;
import java.util.Collection;

public interface ScheduledTaskDaoCustom
{
	Collection<ScheduledTaskEntity> fetchTasksForExecution(ZonedDateTime dateTime);
	//TODO the previous status is added to make sure that no one has changes this status before. Consider versioning
	void changeTasksStatus(String id, TaskStatus newStatus, TaskStatus previousStatus);
}
