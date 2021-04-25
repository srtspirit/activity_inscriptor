package ca.vastier.activityinscriptor.controllers;

import ca.vastier.activityinscriptor.dtos.ScheduledTaskDto;
import ca.vastier.activityinscriptor.services.ScheduledTaskService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

	@GetMapping("/{id}")
	public ScheduledTaskDto getTask(@PathVariable("id") final String id)
	{
		return scheduledTaskService.getTask(id);
	}

	@PutMapping("/{id}")
	public ScheduledTaskDto updateTask(@PathVariable("id") final String id, @RequestBody final ScheduledTaskDto scheduledTaskDto)
	{
		return scheduledTaskService.updateTask(id, scheduledTaskDto);
	}

	@DeleteMapping("/{id}")
	public void deleteTask(@PathVariable("id") final String id)
	{
		scheduledTaskService.deleteTask(id);
	}
}
