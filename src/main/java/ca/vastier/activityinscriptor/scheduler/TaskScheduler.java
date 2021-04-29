package ca.vastier.activityinscriptor.scheduler;

import ca.vastier.activityinscriptor.persistence.daos.CredentialDao;
import ca.vastier.activityinscriptor.persistence.daos.ScheduledTaskDao;
import ca.vastier.activityinscriptor.persistence.entities.ScheduledTaskEntity;
import ca.vastier.activityinscriptor.tasks.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Collection;

import static ca.vastier.activityinscriptor.persistence.entities.ScheduledTaskEntity.TaskStatus.RUNNING;
import static ca.vastier.activityinscriptor.persistence.entities.ScheduledTaskEntity.TaskStatus.SCHEDULED;

@Component("ca.vastier.activityinscriptor.scheduler.TaskScheduler") // the default TaskScheduler is already bound by spring
public class TaskScheduler
{
	private static final Logger LOGGER = LoggerFactory.getLogger(TaskScheduler.class);

	private final ScheduledTaskDao scheduledTaskDao;
	private final CredentialDao credentialDao;
	private final Clock montrealClock;
	private final Clock utcClock;
	private final ApplicationContext applicationContext;

	@Autowired
	public TaskScheduler(final ScheduledTaskDao scheduledTaskDao, final CredentialDao credentialDao,
			@Qualifier("montrealClock") final Clock montrealClock, @Qualifier("utcClock") final Clock utcClock,
			final ApplicationContext applicationContext)
	{
		this.scheduledTaskDao = scheduledTaskDao;
		this.credentialDao = credentialDao;
		this.montrealClock = montrealClock;
		this.utcClock = utcClock;
		this.applicationContext = applicationContext;
	}

	@Scheduled(fixedDelay = 5000)
	public void scanForTasks()
	{
		final Collection<ScheduledTaskEntity> tasks = scheduledTaskDao.fetchTasksForExecution(ZonedDateTime.now(montrealClock));
		LOGGER.debug("Scheduled scan for task ran. Found {} tasks for execution", tasks.size());

		tasks.parallelStream().forEach(task -> {
			final Task taskRunner;
			try
			{
				LOGGER.info("scheduling a task {}", task.getId());
				taskRunner = (Task) applicationContext.getBean(task.getType().name());
				scheduledTaskDao.changeTasksStatus(task.getId(), RUNNING, SCHEDULED);

				try
				{
					taskRunner.run(task.getParameters());
					task.setStatus(ScheduledTaskEntity.TaskStatus.FINISHED);
					scheduledTaskDao.saveTask(task);

					LOGGER.info("Graceful completion of the task {}", task.getId());
				}
				//TODO catch different exceptions including RetryLaterException
				catch (final Throwable e)
				{
					LOGGER.error(e.getMessage());
					task.setStatus(ScheduledTaskEntity.TaskStatus.FAILED);
					task.setMessage(e.getMessage());
					scheduledTaskDao.saveTask(task);
				}
			}
			catch (final BeansException | ClassCastException e)
			{
				LOGGER.error(e.getMessage(), e);
				task.setStatus(ScheduledTaskEntity.TaskStatus.FAILED);
				task.setMessage(e.getMessage());
				scheduledTaskDao.saveTask(task);
			}
		});
	}

	@Scheduled(fixedDelay = 4 * 60 * 60 * 1000) //4 hours
	public void removeOldCoolies()
	{
		credentialDao.removeByExpirationDateLessThan(LocalDateTime.now(utcClock));
	}
}
