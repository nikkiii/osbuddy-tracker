package org.nikkii.rs07.http;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * An HttpRequest for a POST request (using form-urlencoded)
 *
 * @author Nikki
 */
public class HttpPostRequest extends HttpRequest {

	public HttpPostRequest(String url) {
		super(url);
	}

	@Override
	public void execute() throws IOException {
		openConnection();

		connection.setDoOutput(true);
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

		try (OutputStream output = new BufferedOutputStream(openOutputStream())) {
			output.write(parameters.toURLEncodedString().getBytes("UTF-8"));
		}
	}
}
