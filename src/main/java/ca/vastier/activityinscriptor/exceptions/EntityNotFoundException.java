package ca.vastier.activityinscriptor.exceptions;

public class EntityNotFoundException extends RuntimeException
{
	public EntityNotFoundException(final String entityName, final String id)
	{
		super(String.format("entity %s with id %s not found", entityName, id));
	}
}
