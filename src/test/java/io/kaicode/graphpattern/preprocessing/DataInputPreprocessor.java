package io.kaicode.graphpattern.preprocessing;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to preprocess Barts Health Diabetic-Foot project files.
 */
public class DataInputPreprocessor {

	public static void main(String[] args) {
		new DataInputPreprocessor().mapInputToSnomed(
				"input_to_HDAD_optout.csv",// Input file
				"Barts - Diabetic Foot Project - Feature Map_1.tsv",// Map exported from Snap2SNOMED
				"barts-shortened-terms-map.txt",// Snap2SNOMED max code length is 50 so some were shortened
				"instance-data.txt"// Output file
		);
	}

	private void mapInputToSnomed(String instanceData, String localCodeToSnomedMapFile, String shortenedLocalCodeToFullCodeMapFile, String outputFile) {

		Map<String, String> localCodeToSnomedMap = readLocalCodeToSnomedMap(localCodeToSnomedMapFile, shortenedLocalCodeToFullCodeMapFile);

		String expectedHeader = "Result,Criterion,Year,id";
		// Result,Criterion,Year,id
		// 1,Abdominal_aortic_aneurysm,1,52385
		try (BufferedReader reader = new BufferedReader(new FileReader(instanceData));
			 BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {

			String header = reader.readLine();
			if (!expectedHeader.equals(header)) {
				throw new RuntimeException("Input header not as expected.");
			}

			writer.write("instance\tyear\tsnomedId");
			writer.newLine();

			String line;
			while ((line = reader.readLine()) != null) {
				// Result,Criterion,Year,id
				// 0     ,1        ,2   ,3
				String[] columns = line.split(",");
				String localCode = columns[1];
				String year = columns[2];
				String instance = columns[3];
				String snomedCode = localCodeToSnomedMap.get(localCode);
				if (snomedCode != null) {
					writer.write(instance);
					writer.write("\t");
					writer.write(year);
					writer.write("\t");
					writer.write(snomedCode);
					writer.newLine();
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Map<String, String> readLocalCodeToSnomedMap(String localCodeToSnomedMapFile, String shortenedLocalCodeToFullCodeMapFile) {
		Map<String, String> shortenedCodeToFullCodeMap = readMapFile(shortenedLocalCodeToFullCodeMapFile, 0, 1);
		Map<String, String> localCodeToSnomedMap = readMapFile(localCodeToSnomedMapFile, 0, 2);

		for (Map.Entry<String, String> shortenedCodeToFullCode : shortenedCodeToFullCodeMap.entrySet()) {
			String shortenedCode = shortenedCodeToFullCode.getKey();
			String fullCode = shortenedCodeToFullCode.getValue();
			String snomedId = localCodeToSnomedMap.get(shortenedCode);
			if (snomedId != null) {
				localCodeToSnomedMap.put(fullCode, snomedId);
			}
		}
		return localCodeToSnomedMap;
	}

	private Map<String, String> readMapFile(String twoColumnTSVFile, int keyIndex, int valueIndex) {
		Map<String, String> shortenedTermToFullTermMap = new HashMap<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(twoColumnTSVFile))) {
			reader.readLine();
			String line;
			while ((line = reader.readLine()) != null) {
				String[] split = line.split("\t");
				shortenedTermToFullTermMap.put(split[keyIndex], split[valueIndex]);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return shortenedTermToFullTermMap;
	}

}
