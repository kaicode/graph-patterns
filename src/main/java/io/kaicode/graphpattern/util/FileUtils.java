package io.kaicode.graphpattern.util;

import java.io.BufferedReader;
import java.io.IOException;

import static java.lang.String.format;

public class FileUtils {

	public static void readAndAssertHeader(BufferedReader reader, String expectedHeader) throws IOException {
		String actualHeader = reader.readLine();
		if (!expectedHeader.equals(actualHeader)) {
			throw new RuntimeException(format("Unexpected header, expected '%s', got '%s'", expectedHeader, actualHeader));
		}
	}

}
