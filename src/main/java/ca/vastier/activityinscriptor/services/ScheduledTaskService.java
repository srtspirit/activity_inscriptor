package ca.vastier.activityinscriptor.services;

import ca.vastier.activityinscriptor.persistence.daos.ScheduledTaskDao;
import ca.vastier.activityinscriptor.persistence.entities.ScheduledTaskEntity;
import ca.vastier.activityinscriptor.dtos.ScheduledTaskDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

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
		scheduledTaskEntity.setId(UUID.randomUUID().toString());
		final ScheduledTaskEntity savedEntity = scheduledTaskDao.saveTask(scheduledTaskEntity);

		return objectMapper.convertValue(savedEntity, ScheduledTaskDto.class);
	}

	public Collection<ScheduledTaskDto> getAllTasks()
	{
		final Collection<ScheduledTaskEntity> entities = scheduledTaskDao.getAllTasks();
		return entities.stream().map(entity -> objectMapper.convertValue(entity, ScheduledTaskDto.class)).collect(Collectors.toList());
	}
}
