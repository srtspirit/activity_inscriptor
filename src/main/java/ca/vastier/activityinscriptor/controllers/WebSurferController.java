package ca.vastier.activityinscriptor.controllers;

import ca.vastier.activityinscriptor.dtos.LongueuilClassAttendanceDto;
import ca.vastier.activityinscriptor.dtos.LongueuilReservationResponseDto;
import ca.vastier.activityinscriptor.dtos.ScheduledTaskDto;
import ca.vastier.activityinscriptor.services.ScheduledTaskService;
import ca.vastier.activityinscriptor.services.WebSurfer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
public class WebSurferController
{
	private static final String SURF_PATH = "surf/";
	private final WebSurfer webSurfer;
	private final ScheduledTaskService scheduledTaskService;

	@Autowired
	public WebSurferController(final WebSurfer webSurfer, final ScheduledTaskService scheduledTaskService)
	{
		this.webSurfer = webSurfer;
		this.scheduledTaskService = scheduledTaskService;
	}

	@PostMapping(value = SURF_PATH + "ajax/classinfo/booking_confirm", consumes = { MediaType.APPLICATION_FORM_URLENCODED_VALUE })
	@ResponseStatus(HttpStatus.OK)
	public LongueuilReservationResponseDto confirmBooking(
			@ModelAttribute final LongueuilClassAttendanceDto longueuilClassAttendanceDto,
			@RequestHeader(name = "cookie") final String cookies)
	{
		//TODO exception handling
		final int ciSessionStartIndex = cookies.indexOf("ci_session=") + "ci_session=".length();
		if (ciSessionStartIndex == -1)
		{
			return LongueuilReservationResponseDto.builder()
					.code(0)
					.error(true)
					.html("")
					.message("ci_session cookie not found")
					.build();
		}
		int ciSessionEndIndex = cookies.indexOf(";", ciSessionStartIndex);

		final ScheduledTaskDto created;
		try
		{
			created = scheduledTaskService.createTask(longueuilClassAttendanceDto, ciSessionStartIndex == -1 ?
					cookies.substring(ciSessionStartIndex) :
					cookies.substring(ciSessionStartIndex, ciSessionEndIndex));
		}
		catch (final RuntimeException e)
		{
			return LongueuilReservationResponseDto.builder()
					.code(0)
					.error(true)
					.html("")
					.message("ci_session cookie not found")
					.build();
		}

		return LongueuilReservationResponseDto.builder()
				.code(0)
				.error(false)
				.html("")
				.message("Создана заявка для записи с идентификатором " + created.getId()
						+ "\n Запишите его, он понадобится при обращении в ТП")
				.build();
	}

	@PostMapping(value = SURF_PATH + "ajax/classinfo/register_class", produces = APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.OK)
	public LongueuilReservationResponseDto registerClass() throws IOException
	{
		//TODO move to surfer class
		final String html = Files.readString(Path.of("templates/confirmation.html"));

		return LongueuilReservationResponseDto.builder().error(false).code(0).html(html).build();
	}

	@GetMapping(SURF_PATH + "file/js/{name}")
	public ResponseEntity<Resource> downloadFile(@PathVariable("name") final String fileName) throws IOException
	{
		final File file = new File("js/" + fileName);
		final ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(Paths.get(file.getAbsolutePath())));

		return ResponseEntity.ok().contentLength(file.length()).contentType(MediaType.APPLICATION_OCTET_STREAM).body(resource);
	}

	@GetMapping(SURF_PATH + "home/dashboard")
	public ResponseEntity<String> getDashboard(@RequestHeader MultiValueMap<String, String> headers)
	{
		return webSurfer.getDashboard(headers);
	}

	@PostMapping(SURF_PATH + "home/login")
	public ResponseEntity<String> login(@RequestBody String body, @RequestHeader MultiValueMap<String, String> headers)
	{
		return webSurfer.login(headers, body);
	}

	@RequestMapping(value = SURF_PATH + "**", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public ResponseEntity<Object> surf(HttpServletRequest request, @RequestHeader MultiValueMap<String, String> headers,
			@RequestBody(required = false) String body) throws IOException
	{
		String fullPath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
		return webSurfer.surf(request.getMethod(), fullPath.substring(SURF_PATH.length() + 1), headers, body);
	}
}
