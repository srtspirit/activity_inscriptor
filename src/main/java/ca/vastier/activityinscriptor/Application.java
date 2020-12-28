package ca.vastier.activityinscriptor;

import ca.vastier.activityinscriptor.tasks.InscriptionTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

@SpringBootApplication
public class Application
{
	private final static Logger LOGGER = LoggerFactory.getLogger(Application.class);

	public static void main(final String[] args)
	{
		SpringApplication.run(Application.class, args);
	}

	@EventListener(classes = { ApplicationReadyEvent.class })
	public void test(final ApplicationReadyEvent applicationReadyEvent)
	{
		Properties properties = null;
		try
		{
			File file = new File("inscription.data");
			FileInputStream fileInput = new FileInputStream(file);
			properties = new Properties();
			properties.load(fileInput);
			fileInput.close();

			applicationReadyEvent.getApplicationContext()
					.getBean(InscriptionTask.class)
					.run(properties.keySet().stream().map(Object::toString).collect(Collectors.toMap(identity(), properties::get)));
		}
		catch (FileNotFoundException e)
		{
			LOGGER.error(
					"could not open file inscription.data. Please make sure you have this file next to the application and have permission to read");
		}
		catch (IOException e)
		{
			LOGGER.error(e.getMessage(), e);
		}
	}
}
