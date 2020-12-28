package ca.vastier.activityinscriptor;

import ca.vastier.activityinscriptor.tasks.InscriptionTask;
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
	public static void main(final String[] args)
	{
		System.out.println("hui1");
		SpringApplication.run(Application.class, args);
		System.out.println("hui2");
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

		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		applicationReadyEvent.getApplicationContext()
				.getBean(InscriptionTask.class)
				.run(properties.keySet().stream().map(Object::toString).collect(Collectors.toMap(identity(), properties::get)));
	}
}
