package ca.vastier.activityinscriptor.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LongueuilClassAttendanceDto
{
	private String id; //class_id
	//TODO work on the API as dt seems to be redundant
	private String dt; //class_date yyyy-MM-dd
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	private LocalDateTime startDateTime;
	private String timeZoneId;
	private String contract_id;
	private String present_number;

	//some staff about COVID-19. Hello from 2021)))))))
	private boolean question_1;
	private boolean question_2;
	private boolean question_3;
}
