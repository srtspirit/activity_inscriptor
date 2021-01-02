package ca.vastier.activityinscriptor.daos;

import lombok.Getter;
import lombok.Setter;

import java.time.Period;
import java.time.ZonedDateTime;
import java.util.Map;

@Getter
@Setter
public class ScheduledTaskEntity
{
	private String id;
	private ZonedDateTime startTime;
	private Period requiredPreparationTime;
	private Map<String, Object> parameters;
	private TaskType type;
	private TaskStatus status;

	public static enum TaskType
	{
		ACTIVITY_INSCRIPTOR;
	}

	public static enum TaskStatus
	{
		SCHEDULED, RUNNING, FINISHED, FAILED
	}
}
