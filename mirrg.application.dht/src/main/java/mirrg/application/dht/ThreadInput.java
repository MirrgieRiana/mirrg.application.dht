package mirrg.application.dht;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.stream.Collectors;

import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import mirrg.helium.standard.hydrogen.struct.Tuple;

public class ThreadInput extends Thread
{

	private PrintStream out;
	private String color;

	public ThreadInput(String name, String color, OutputStream out, String encoding) throws UnsupportedEncodingException
	{
		super(name);
		this.color = color;
		this.out = new PrintStream(out, true, encoding);
	}

	@Override
	public void run()
	{
		try {
			HttpServer httpServer = HttpServer.create(new InetSocketAddress(Main.get("hostname"), Main.getAsInt("port")), Main.getAsInt("backlog"));
			HttpContext context = httpServer.createContext("/", e -> {

				if (e.getRequestURI().getPath().toString().matches("/api(|/.*)")) {

					if (e.getRequestURI().getPath().toString().matches("/api/log")) {
						send(e, String.format(
							"<table style='font-family: monospace; white-space: nowrap;'>%s</table>",
							Main.lines.stream()
								.map(t -> String.format(
									"<tr style=\"color: %s;\"><td>%s</td><td><b>%s</b></td><td>%s</td></tr>",
									t.getY().color,
									t.getX(),
									t.getY().type,
									t.getY().line))
								.collect(Collectors.joining())));
					} else if (e.getRequestURI().getPath().toString().matches("/api/send")) {
						String query = e.getRequestURI().getQuery();
						if (query == null) {
							send(e, 400, "400");
						} else {
							Main.push(new Entry(color, getName().toUpperCase(), query));
							out.println(query);
							send(e, "Success[" + query + "]");
						}
					} else {
						send(e, 404, "404");
					}

				} else if (e.getRequestURI().getPath().toString().matches("/")) {
					redirect(e, "/index.html");
				} else {
					sendFile(e, ThreadInput.class.getResource("html" + e.getRequestURI().getPath()));
				}

			});
			if (Main.getAsBoolean("needAuthentication")) {
				context.setAuthenticator(new BasicAuthenticator("Controller") {
					@Override
					public boolean checkCredentials(String arg0, String arg1)
					{
						if (!Main.get("user").equals(arg0)) return false;
						if (!Main.get("password").equals(arg1)) return false;
						return true;
					}
				});
			}
			httpServer.start();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void redirect(HttpExchange e, String string) throws IOException
	{
		e.getResponseHeaders().add("Location", string);
		e.sendResponseHeaders(301, 0);
		e.getResponseBody().close();
	}

	private void send(HttpExchange e, String text) throws IOException
	{
		send(e, 200, "text/html", text, "utf-8");
	}

	private void send(HttpExchange e, int code, String text) throws IOException
	{
		send(e, code, "text/html", text, "utf-8");
	}

	private void send(HttpExchange e, int code, String contentType, String text, String charset) throws IOException
	{
		e.getResponseHeaders().add("Content-Type", contentType + "; charset= " + charset);
		byte[] bytes = text.getBytes(charset);
		e.sendResponseHeaders(code, bytes.length);
		e.getResponseBody().write(bytes);
		e.getResponseBody().close();
	}

	private void sendFile(HttpExchange e, URL url) throws IOException
	{
		try {
			InputStream in = url.openStream();

			ArrayList<Tuple<byte[], Integer>> buffers = new ArrayList<>();
			while (true) {
				byte[] buffer = new byte[4000];
				int len = in.read(buffer);
				if (len == -1) break;
				buffers.add(new Tuple<>(buffer, len));
			}
			in.close();

			e.sendResponseHeaders(200, buffers.stream()
				.mapToInt(Tuple::getY)
				.sum());
			for (Tuple<byte[], Integer> buffer : buffers) {
				e.getResponseBody().write(buffer.getX(), 0, buffer.getY());
			}
			e.getResponseBody().close();
		} catch (IOException e2) {
			send(e, 404, "404");
		}
	}

}
