package ca.vastier.activityinscriptor.services;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class FileReader
{
	public String readFileAsString(final String path) throws IOException
	{
		return Files.readString(Path.of(path));
	}
}
