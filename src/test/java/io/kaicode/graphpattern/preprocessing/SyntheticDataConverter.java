package io.kaicode.graphpattern.preprocessing;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// Utility method to convert synthetic data from https://github.com/IHTSDO/health-data-analytics into the format for this project.
public class SyntheticDataConverter {

	public static void main(String[] args) throws IOException {
		String inputPath = "../health-data-analytics/patient-data-for-import/generated-patients.ndjson";
		String outputDir = "synthetic-data";
		String cohortBConcept = "419099009";
		String fhirTerminologyServer = "https://snowstorm.ihtsdotools.org/fhir";
		new SyntheticDataConverter().run(inputPath, outputDir, cohortBConcept, fhirTerminologyServer);
	}

	private void run(String syntheticDataFilePath, String outputDirPath, String cohortBConcept, String fhirTerminologyServer) throws IOException {
		System.out.println("Loading descendants and self of cohortB concept " + cohortBConcept + " from terminology server " + fhirTerminologyServer);
		RestTemplate restTemplate = new RestTemplateBuilder().rootUri(fhirTerminologyServer).build();
		ResponseEntity<ValueSet> valueSetExpansion =
				restTemplate.getForEntity("/ValueSet/$expand?count=1000&url=http://snomed.info/sct?fhir_vs=ecl/<<" + cohortBConcept, ValueSet.class);
		List<Long> cohortBConcepts = valueSetExpansion.getBody().getExpansion().getContains().stream()
				.map(ValueSet.Expansion.Code::getCode)
				.map(Long::parseLong)
				.toList();
		System.out.printf("Found %s concepts%n", cohortBConcepts.size());

		File ndjson = new File(syntheticDataFilePath);
		File outputDir = new File(outputDirPath);
		if (!outputDir.exists() && !outputDir.mkdirs()) {
			throw new IOException("Failed to create output directory");
		}

		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		int patients = 0;
		int cohorts = 0;
		try (BufferedReader reader = new BufferedReader(new FileReader(ndjson));
			 BufferedWriter patientWriter = new BufferedWriter(new FileWriter(new File(outputDir, "patients.txt")));
			 BufferedWriter cohortWriter = new BufferedWriter(new FileWriter(new File(outputDir, "cohorts.txt")))) {

			patientWriter.write("instance\tyear\tcode");
			patientWriter.newLine();

			cohortWriter.write("instance\tcohort");
			cohortWriter.newLine();

			String line;
			while ((line = reader.readLine()) != null) {
				Patient patient = objectMapper.readValue(line, Patient.class);

				String cohort = "A";
				if (patient.getEvents() != null) {
					Set<Long> patientConcepts = patient.getEvents().stream().map(Patient.Encounter::getConceptId).collect(Collectors.toSet());

					for (Long patientConcept : patientConcepts) {
						if (cohortBConcepts.contains(patientConcept)) {
							cohort = "B";
						}
						patientWriter.write(patient.getRoleId());
						patientWriter.write("\t");
						patientWriter.write("1");// hardcoded
						patientWriter.write("\t");
						patientWriter.write(patientConcept.toString());
						patientWriter.newLine();
						patients++;
					}
				}

				cohortWriter.write(patient.getRoleId());
				cohortWriter.write("\t");
				cohortWriter.write(cohort);
				cohortWriter.newLine();
				cohorts++;
			}
		}
		System.out.printf("Written %s patients, %s cohorts", patients, cohorts);
	}

	private static final class ValueSet {
		private Expansion expansion;

		public Expansion getExpansion() {
			return expansion;
		}

		private static class Expansion {
			private List<Code> contains;

			public List<Code> getContains() {
				return contains;
			}

			private static class Code {
				private String code;

				public String getCode() {
					return code;
				}
			}
		}
	}

	private static final class Patient {

		private String roleId;
		private List<Encounter> events;

		public String getRoleId() {
			return roleId;
		}

		public List<Encounter> getEvents() {
			return events;
		}

		private static final class Encounter {

			private Long conceptId;

			public Long getConceptId() {
				return conceptId;
			}
		}
	}

}
