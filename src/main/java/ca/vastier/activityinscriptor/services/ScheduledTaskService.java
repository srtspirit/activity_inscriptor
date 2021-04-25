package ca.vastier.activityinscriptor.services;

import ca.vastier.activityinscriptor.dtos.ScheduledTaskDto;
import ca.vastier.activityinscriptor.exceptions.EntityNotFoundException;
import ca.vastier.activityinscriptor.persistence.daos.ScheduledTaskDao;
import ca.vastier.activityinscriptor.persistence.entities.ScheduledTaskEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

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

	public ScheduledTaskDto getTask(final String id)
	{
		final ScheduledTaskEntity scheduledTaskEntity = scheduledTaskDao.findTaskById(id)
				.orElseThrow(() -> new EntityNotFoundException("task", id));

		return objectMapper.convertValue(scheduledTaskEntity, ScheduledTaskDto.class);
	}

	public ScheduledTaskDto updateTask(final String id, final ScheduledTaskDto scheduledTaskDto)
	{
		if (!scheduledTaskDao.doesTaskWithIdExist(id))
		{
			throw new EntityNotFoundException("task", id);
		}

		final ScheduledTaskEntity saved = scheduledTaskDao.saveTask(
				objectMapper.convertValue(scheduledTaskDto, ScheduledTaskEntity.class));
		return objectMapper.convertValue(saved, ScheduledTaskDto.class);
	}

	public void deleteTask(final String id)
	{
		if (!scheduledTaskDao.doesTaskWithIdExist(id))
		{
			throw new EntityNotFoundException("task", id);
		}

		scheduledTaskDao.deleteTaskById(id);
	}
}
