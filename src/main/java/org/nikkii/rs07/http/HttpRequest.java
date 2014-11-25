package org.nikkii.rs07.http;

import org.nikkii.rs07.http.data.RequestData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * A Basic HTTP Request, All other requests should extend this
 *
 * @author Nikki
 */
public abstract class HttpRequest implements AutoCloseable {
	/**
	 * The URL String. We aren't using URL objects just so it's easier to manipulate in GET requests, though it'd be a good idea to allow a URL constructor.
	 */
	protected String url;

	/**
	 * The maximum output in bytes/second, used by the TokenBucket
	 */
	private final int rateLimit;

	/**
	 * The UserAgent for this request
	 */
	private String userAgent;

	/**
	 * The connection instance
	 */
	protected HttpURLConnection connection;

	/**
	 * A map of header elements. This could be done with guava if we wanted to add another dependency...
	 */
	private Map<String, List<Object>> headers = new HashMap<>();

	/**
	 * A list of request parameters (RequestData is a simple wrapper which allows chaining)
	 */
	protected RequestData parameters = new RequestData();

	/**
	 * Construct a new HttpRequest from the specified url string
	 *
	 * @param url The URL
	 */
	public HttpRequest(String url) {
		this(url, -1);
	}

	/**
	 * Construct a new HttpRequest from the specified url string
	 *
	 * @param url The URL
	 * @param rateLimit The bytes per second to limit output to
	 */
	public HttpRequest(String url, int rateLimit) {
		this.url = url;
		this.rateLimit = rateLimit;
	}

	/**
	 * Construct a new HttpRequest from the specified url string
	 *
	 * @param url The URL
	 */
	public HttpRequest(URL url) {
		this(url.toString(), -1);
	}

	/**
	 * Construct a new HttpRequest from the specified url string
	 *
	 * @param url The URL
	 * @param rateLimit The bytes per second to limit output to
	 */
	public HttpRequest(URL url, int rateLimit) {
		this(url.toString(), rateLimit);
	}

	/**
	 * Called automatically when using getConnection/getResponse* to execute the request
	 *
	 * @throws IOException If an error occurred while executing
	 */
	public abstract void execute() throws IOException;

	/**
	 * Opens the connection and sets the uesr agent, headers, etc.
	 *
	 * @throws IOException If an error occurred while opening the connection
	 */
	protected void openConnection() throws IOException {
		connection = (HttpURLConnection) new URL(url).openConnection();

		if (userAgent != null) {
			connection.setRequestProperty("User-Agent", userAgent);
		}

		if (!headers.isEmpty()) {
			for (Map.Entry<String, List<Object>> e : headers.entrySet()) {
				for (Object o : e.getValue()) {
					connection.addRequestProperty(e.getKey(), o.toString());
				}
			}
		}
	}

	/**
	 * Open an OutputStream to the URLConnection
	 *
	 * @return A normal OutputStream if not limited, or a TokenBucket one if it is.
	 * @throws IOException If an error occurs while opening the output
	 */
	protected OutputStream openOutputStream() throws IOException {
		if (connection == null) {
			throw new IllegalStateException("Connection must be opened before opening the OutputStream");
		}
		OutputStream output = connection.getOutputStream();

		return output;
	}

	/**
	 * Get the connection object. This is useless except when called after execute or getResponse*
	 *
	 * @return The connection object
	 */
	public HttpURLConnection getConnection() {
		return connection;
	}

	/**
	 * Get the HTTP response code.
	 * This will execute the request if it hasn't already.
	 *
	 * @return The response code from the connection
	 * @throws IOException If an error occurred while executing the request.
	 */
	public int getResponseCode() throws IOException {
		checkConnection();
		return connection.getResponseCode();
	}

	/**
	 * Gets the HTTP response as an InputStream
	 *
	 * @return The response stream
	 */
	public InputStream getResponseStream() throws IOException {
		checkConnection();
		return connection.getInputStream();
	}

	/**
	 * Get the HTTP response body as a string.
	 * This will execute the request if it hasn't already.
	 * TODO: Do we want to make sure there's no ending \n?
	 *
	 * @return The response string
	 * @throws IOException If an error occurred while executing the request.
	 */
	public String getResponseBody() throws IOException {
		checkConnection();

		StringBuilder builder = new StringBuilder();

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
			char[] buf = new char[1024];
			int read;
			while ((read = reader.read(buf, 0, buf.length)) > -1) {
				builder.append(buf, 0, read);
			}
		}

		return builder.toString();
	}

	/**
	 * Gets a header from the response
	 *
	 * @param name The request header name
	 * @return The request header
	 */
	public String getResponseHeader(String name) throws IOException {
		checkConnection();
		return connection.getHeaderField(name);
	}

	/**
	 * Checks if the connection has been initialized, and if not initialize and execute the request.
	 *
	 * @throws IOException If an error occurred while executing the request
	 */
	private void checkConnection() throws IOException {
		if (connection == null) {
			execute();
		}
	}

	/**
	 * Add a request parameter (GET or POST) to the request
	 *
	 * @param key The parameter name
	 * @param value The parameter value (Anything with toString, or MultipartFile for a request)
	 */
	public void addParameter(String key, Object value) {
		parameters.put(key, value);
	}

	/**
	 * Set the parameter object
	 *
	 * @param parameters The new parameter object
	 */
	public void setParameters(RequestData parameters) {
		this.parameters = parameters;
	}

	/**
	 * Add an http request header to this request
	 *
	 * @param name The request header name
	 * @param value The request header value
	 */
	public void addHeader(String name, Object value) {
		List<Object> list = headers.get(name);
		if (list == null) {
			headers.put(name, list = new LinkedList<>());
		}
		list.add(value);
	}

	/**
	 * Set the HTTP User agent
	 *
	 * @param userAgent The user agent
	 */
	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	/**
	 * Close the connection (used in AutoCloseable)
	 */
	@Override
	public void close() {
		connection.disconnect();
	}
}
