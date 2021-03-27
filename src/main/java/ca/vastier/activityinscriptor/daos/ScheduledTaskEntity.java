package ca.vastier.activityinscriptor.daos;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Period;
import java.time.ZonedDateTime;
import java.util.Map;

@Data
@Builder
@Document(collection = "tasks")
public class ScheduledTaskEntity
{
	@Id
	private String id;
	private ZonedDateTime startTime;
	private Period requiredPreparationTime;
	private Map<String, Object> parameters;
	private TaskType type;
	private TaskStatus status;
	private String message;

	public static enum TaskType
	{
		ACTIVITY_INSCRIPTOR;
	}

	public static enum TaskStatus
	{
		SCHEDULED, RUNNING, FINISHED, FAILED
	}
}
