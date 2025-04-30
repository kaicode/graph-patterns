package io.kaicode.graphpattern.preprocessing;

import java.io.*;

// Utility method to convert an RF2 snapshot to the input format for this project
public class SnomedGraphConverter {

	public static void main(String[] args) throws IOException {
		String snomedReleaseUnzippedDirectoryPath = "../../release/SnomedCT_InternationalRF2_PRODUCTION_20240101T120000Z";
		String outputDirectoryPath = "knowledge-graph";
		new SnomedGraphConverter().run(snomedReleaseUnzippedDirectoryPath, outputDirectoryPath);
	}

	private void run(String snomedReleaseUnzippedDirectoryPath, String outputDirectoryPath) throws IOException {
		File dir = new File(snomedReleaseUnzippedDirectoryPath);
		File descriptionSnapshot = findFile(dir, "sct2_Description_Snapshot-en");
		File relationshipSnapshot = findFile(dir, "sct2_Relationship_Snapshot");

		File outputDirectory = new File(outputDirectoryPath);
		if (!outputDirectory.isDirectory() && !outputDirectory.mkdirs()) {
			throw new IOException("Failed to create output directory " + outputDirectory);
		}

		File codeLabelsFile = new File(outputDirectory, "code-labels.txt");
		try (BufferedReader descriptionReader = new BufferedReader(new FileReader(descriptionSnapshot));
			 BufferedWriter termWriter = new BufferedWriter(new FileWriter(codeLabelsFile))) {

			termWriter.write("code\tlabel");
			termWriter.newLine();
			String line;
			int a = 0;
			while ((line = descriptionReader.readLine()) != null) {
				String[] split = line.split("\t");
				// id	effectiveTime	active	moduleId	conceptId	languageCode	typeId	term	caseSignificanceId
				// 0	1				2		3			4			5				6		7		8
				// If active FSN (PT would be better but harder to figure out)
				if ("1".equals(split[2]) && "900000000000003001".equals(split[6])) {
					termWriter.write(split[4]);
					termWriter.write("\t");
					termWriter.write(split[7]);
					termWriter.newLine();
					a++;
				}
			}
			System.out.printf("Wrote %s terms to %s%n", a, codeLabelsFile.getPath());
		}
		File childParentFile = new File(outputDirectory, "child-parent.txt");
		try (BufferedReader relationshipReader = new BufferedReader(new FileReader(relationshipSnapshot));
			 BufferedWriter childParentWriter = new BufferedWriter(new FileWriter(childParentFile))) {

			childParentWriter.write("child\tparent");
			childParentWriter.newLine();
			String line;
			int a = 0;
			while ((line = relationshipReader.readLine()) != null) {
				String[] split = line.split("\t");
				// id	effectiveTime	active	moduleId	sourceId	destinationId	relationshipGroup	typeId	characteristicTypeId	modifierId
				// 0	1				2		3			4			5				6					7		8						9
				// if active is-a relationship
				if ("1".equals(split[2]) && "116680003".equals(split[7])) {
					childParentWriter.write(split[4]);
					childParentWriter.write("\t");
					childParentWriter.write(split[5]);
					childParentWriter.newLine();
					a++;
				}
			}
			System.out.printf("Wrote %s child-parent relationships to %s%n", a, childParentFile.getPath());
		}

		System.out.println("Complete");
	}

	private File findFile(File dir, String filenamePrefix) throws IOException {
		File file = doFindFile(dir, filenamePrefix);
		if (file == null) {
			throw new IOException("Failed to find file with prefix " + filenamePrefix);
		}
		return file;
	}

	private File doFindFile(File dir, String filenamePrefix) {
		File[] files = dir.listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.isDirectory()) {
					File found = doFindFile(file, filenamePrefix);
					if (found != null) {
						return found;
					}
				} else if (file.getName().startsWith(filenamePrefix)) {
					return file;
				}
			}
		}
		return null;
	}

}
