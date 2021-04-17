package ca.vastier.activityinscriptor.persistence.namespaces;

public final class ScheduledTaskEntityNamespace
{
	private ScheduledTaskEntityNamespace()
	{
	}

	public static final String TABLE_NAME = "tasks";
	public static class Columns
	{
		public static final String ID = "id";
		public static final String START_TIME = "startTime";
		public static final String REQUIRED_PREPARATION_TIME = "requiredPreparationTime";
		public static final String PARAMETERS = "parameters";
		public static final String TYPE = "type";
		public static final String STATUS = "status";
		public static final String MESSAGE = "message";
	}
}
