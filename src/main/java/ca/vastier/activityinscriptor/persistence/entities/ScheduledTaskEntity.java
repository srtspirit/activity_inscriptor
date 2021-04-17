package ca.vastier.activityinscriptor.persistence.entities;

import ca.vastier.activityinscriptor.persistence.namespaces.ScheduledTaskEntityNamespace;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = ScheduledTaskEntityNamespace.TABLE_NAME)
public class ScheduledTaskEntity
{
	@Id
	@Field(name = ScheduledTaskEntityNamespace.Columns.ID)
	private String id;
	@Field(name = ScheduledTaskEntityNamespace.Columns.START_TIME)
	private LocalDateTime startTime;
	@Field(name = ScheduledTaskEntityNamespace.Columns.REQUIRED_PREPARATION_TIME)
	private Long requiredPreparationTime;
	@Field(name = ScheduledTaskEntityNamespace.Columns.PARAMETERS)
	private Map<String, Object> parameters;
	@Field(name = ScheduledTaskEntityNamespace.Columns.TYPE)
	private TaskType type;
	@Field(name = ScheduledTaskEntityNamespace.Columns.STATUS)
	private TaskStatus status;
	@Field(name = ScheduledTaskEntityNamespace.Columns.MESSAGE)
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
