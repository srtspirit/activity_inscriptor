package ca.vastier.activityinscriptor.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LongueuilReservationResponseDto
{
	private boolean error;
	private Integer code;
	private String html;
	private String message;
}
