package ca.vastier.activityinscriptor.scheduler;

import ca.vastier.activityinscriptor.daos.ScheduledTaskDao;
import ca.vastier.activityinscriptor.daos.ScheduledTaskEntity;
import ca.vastier.activityinscriptor.tasks.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Collection;

public class TaskScheduler
{
	private static final Logger LOGGER = LoggerFactory.getLogger(TaskScheduler.class);

	private final ScheduledTaskDao scheduledTaskDao;
	private final Clock clock;
	private final ApplicationContext applicationContext;

	@Autowired
	public TaskScheduler(final ScheduledTaskDao scheduledTaskDao, final Clock clock, final ApplicationContext applicationContext)
	{
		this.scheduledTaskDao = scheduledTaskDao;
		this.clock = clock;
		this.applicationContext = applicationContext;
	}

	public void scanForTasks()
	{
		final Collection<ScheduledTaskEntity> tasks = scheduledTaskDao.fetchTasksForExecution(ZonedDateTime.now(clock));
		tasks.forEach(task -> {
			final Task taskRunner;
			try
			{
				LOGGER.info("scheduling a task {}", task.getId());
			 taskRunner =	(Task) applicationContext.getBean(task.getType().name());
			 //TODO use a normal thread pool
			 new Thread(() -> {
			 	try
				{
					taskRunner.run(task.getParameters());
				}
			 	//TODO catch different exceptions including RetryLaterException
			 	catch (final Throwable e)
				{
					LOGGER.error(e.getMessage(), e);
					task.setStatus(ScheduledTaskEntity.TaskStatus.FAILED);
					scheduledTaskDao.saveTask(task);
				}

			 	task.setStatus(ScheduledTaskEntity.TaskStatus.FINISHED);
			 	scheduledTaskDao.saveTask(task);
			 }).start();
			}
			catch (final BeansException | ClassCastException e)
			{
				LOGGER.error(e.getMessage(), e);
			}
		});
	}
}
