package ca.vastier.activityinscriptor.services;

import lombok.NonNull;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class FileReader
{
	public String readFileAsString(@NonNull Charset charset, @NonNull final String path, String... substituteArguments)
	{
		//TODO make substitution parametrized (ie %username% -> username). Or look for libraries
		try
		{
			final String readString = Files.readString(Path.of(path), charset);

			if (substituteArguments != null && substituteArguments.length > 0)
			{
				return String.format(readString, (Object[]) substituteArguments);
			}
			else
			{
				return readString;
			}

		}
		catch (IOException e)
		{
			//TODO good exception
			throw new RuntimeException(e.getMessage());
		}
	}

	public String readFileAsString(@NonNull final String path, String... substituteArguments)
	{
		return readFileAsString(StandardCharsets.UTF_8, path, substituteArguments);
	}
}
