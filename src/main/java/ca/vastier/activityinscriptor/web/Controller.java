package ca.vastier.activityinscriptor.web;

import ca.vastier.activityinscriptor.tasks.InscriptionTask;
import ca.vastier.activityinscriptor.tasks.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class Controller
{
	private final InscriptionTask task;

	@Autowired
	public Controller(final InscriptionTask task)
	{
		this.task = task;
	}

	@PostMapping("trggier")
	public void trigger()
	{
		Map<String, Object> param = Map.of("username", "srt.vlasov@gmail.com",
				"password", "33!100Aaa",
				"domain", "longueuil",
				"eventId", "223114",
				"dateTime", "2020-12-29");
		task.run(param);
	}
}
