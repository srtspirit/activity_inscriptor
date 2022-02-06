package ca.vastier.activityinscriptor.services;

import ca.vastier.activityinscriptor.dtos.LongueuilClassAttendanceDto;
import ca.vastier.activityinscriptor.dtos.ScheduledTaskDto;
import ca.vastier.activityinscriptor.exceptions.EntityNotFoundException;
import ca.vastier.activityinscriptor.persistence.daos.CredentialDao;
import ca.vastier.activityinscriptor.persistence.daos.ScheduledTaskDao;
import ca.vastier.activityinscriptor.persistence.entities.CredentialEntity;
import ca.vastier.activityinscriptor.persistence.entities.ScheduledTaskEntity;
import ca.vastier.activityinscriptor.tasks.InscriptionTaskConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static ca.vastier.activityinscriptor.tasks.InscriptionTaskConstants.PropertyNames.*;

@Service
public class ScheduledTaskService
{
	private final ScheduledTaskDao scheduledTaskDao;
	private final CredentialDao credentialDao;
	private final ObjectMapper objectMapper;

	@Autowired
	public ScheduledTaskService(final ScheduledTaskDao scheduledTaskDao, final CredentialDao credentialDao)
	{
		this.scheduledTaskDao = scheduledTaskDao;
		this.credentialDao = credentialDao;
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

	public ScheduledTaskDto createTask(final LongueuilClassAttendanceDto longueuilClassAttendanceDto, final String sessionCookie)
	{
		//TODO better exception handling
		final CredentialEntity credentialEntity = credentialDao.findByCookieValue(sessionCookie)
				.stream()
				.findFirst()
				.orElseThrow(() -> new RuntimeException("Could not find credentials"));

		//@formatter:off
		final ScheduledTaskDto scheduledTaskDto = ScheduledTaskDto.builder()
				.parameters(Map.of(
						USERNAME, credentialEntity.getLogin(),
						PASSWORD, credentialEntity.getPassword(),
						DOMAIN, "longueuil",
						EVENT_ID, longueuilClassAttendanceDto.getId(),
						DATE, longueuilClassAttendanceDto.getDt(),
						NUMBER_OF_VISITORS, longueuilClassAttendanceDto.getPresent_number(),
						DELAY, "450"))
				.requiredPreparationTime(7000L)
				//TODO inscription starts 48h prior to the actual activity. Check the edge case where this period includes changing daylight saving time
				.startTime(longueuilClassAttendanceDto.getStartDateTime().minus(48L, ChronoUnit.HOURS))
				.type(ScheduledTaskEntity.TaskType.ACTIVITY_INSCRIPTOR)
				.status(ScheduledTaskEntity.TaskStatus.SCHEDULED)
				.build();
		//@formatter:on

		return createTask(scheduledTaskDto);
	}
}
