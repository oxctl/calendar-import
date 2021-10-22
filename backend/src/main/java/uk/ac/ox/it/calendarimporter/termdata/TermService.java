package uk.ac.ox.it.calendarimporter.termdata;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * This supports getting the reference data from the dynamics driven endpoints.
 * This doesn't do any filtering or anything as other services should do that.
 */
@Service
public class TermService {

	@Autowired
	private WebClient webClient;

	@Value("${dynamics.year.url}")
	private String yearUrl;

	@Value("${dynamics.term.url}")
	private String termUrl;
	
	public List<AcademicYear> getYears() {
		Mono<List<AcademicYear>> response = webClient.get()
				.uri(yearUrl)
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.bodyToMono(new ParameterizedTypeReference<List<AcademicYear>>() {});
		return response.block();
	}

	public List<AcademicYearTerm> getTerms() {
		Mono<List<AcademicYearTerm>> response = webClient.get()
				.uri(termUrl)
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.bodyToMono(new ParameterizedTypeReference<List<AcademicYearTerm>>() {});
		return response.block();
	}

}
