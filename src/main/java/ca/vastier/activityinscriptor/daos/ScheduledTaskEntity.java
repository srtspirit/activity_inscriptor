package ca.vastier.activityinscriptor.daos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "tasks")
public class ScheduledTaskEntity
{
	@Id
	private String id;
	private LocalDateTime startTime;
	private Long requiredPreparationTime;
	private Map<String, Object> parameters;
	private TaskType type;
	private TaskStatus status;
	private String message;

	public enum TaskType
	{
		ACTIVITY_INSCRIPTOR
	}

	public enum TaskStatus
	{
		SCHEDULED, RUNNING, FINISHED, FAILED
	}
}
