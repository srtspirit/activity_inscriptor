package ca.vastier.activityinscriptor.controllers;

import ca.vastier.activityinscriptor.dtos.ScheduledTaskDto;
import ca.vastier.activityinscriptor.services.ScheduledTaskService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("tasks")
public class ScheduledTaskController
{
	private final ScheduledTaskService scheduledTaskService;

	public ScheduledTaskController(final ScheduledTaskService scheduledTaskService)
	{
		this.scheduledTaskService = scheduledTaskService;
	}

	@PostMapping
	public ScheduledTaskDto createTask(@RequestBody final ScheduledTaskDto scheduledTaskDto)
	{
		return scheduledTaskService.createTask(scheduledTaskDto);
	}
}
