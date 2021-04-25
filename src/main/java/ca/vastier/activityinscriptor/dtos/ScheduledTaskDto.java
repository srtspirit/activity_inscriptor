package ca.vastier.activityinscriptor.dtos;

import ca.vastier.activityinscriptor.persistence.entities.ScheduledTaskEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledTaskDto
{
	private String id;
	//TODO validation
	//TODO find a good way to work with time
	private LocalDateTime startTime;
	@Builder.Default
	private Long requiredPreparationTime = 7000L;
	private Map<String, Object> parameters;
	private ScheduledTaskEntity.TaskType type;
	private ScheduledTaskEntity.TaskStatus status;
	private String message;
}
