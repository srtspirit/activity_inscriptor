package ca.vastier.activityinscriptor.daos;

import java.time.ZonedDateTime;
import java.util.Collection;

public interface ScheduledTaskDaoCustom
{
	Collection<ScheduledTaskEntity> fetchTasksForExecution(ZonedDateTime dateTime);
	void changeTasksStatus(String id, ScheduledTaskEntity.TaskStatus newStatus, ScheduledTaskEntity.TaskStatus previousStatus);
}
