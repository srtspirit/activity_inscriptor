package ca.vastier.activityinscriptor.services;

import ca.vastier.activityinscriptor.daos.ScheduledTaskDao;
import ca.vastier.activityinscriptor.daos.ScheduledTaskEntity;
import ca.vastier.activityinscriptor.dtos.ScheduledTaskDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class ScheduledTaskService
{
	private final ScheduledTaskDao scheduledTaskDao;
	private final ObjectMapper objectMapper;

	@Autowired
	public ScheduledTaskService(final ScheduledTaskDao scheduledTaskDao)
	{
		this.scheduledTaskDao = scheduledTaskDao;
		objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
	}

	public ScheduledTaskDto createTask(final ScheduledTaskDto scheduledTaskDto)
	{
		final ScheduledTaskEntity scheduledTaskEntity = objectMapper.convertValue(scheduledTaskDto, ScheduledTaskEntity.class);
		final ScheduledTaskEntity savedEntity = scheduledTaskDao.saveTask(scheduledTaskEntity);
		savedEntity.setRequiredPreparationTime(Duration.ofSeconds(5));

		return objectMapper.convertValue(savedEntity, ScheduledTaskDto.class);
	}
}
