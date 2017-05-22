package mirrg.application.dht;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Properties;

import mirrg.helium.standard.hydrogen.struct.Tuple;

public class Main
{

	private static Properties properties = new Properties();
	static {
		try {
			properties.load(new FileInputStream("dht.properties"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static String get(String key)
	{
		return properties.getProperty(key);
	}

	public static int getAsInt(String key)
	{
		return Integer.parseInt(properties.getProperty(key), 10);
	}

	public static boolean getAsBoolean(String key)
	{
		return Boolean.parseBoolean(properties.getProperty(key));
	}

	//

	private static final int VISIBLE_LOG_LINE_COUNT = 500;
	public static LinkedList<Tuple<Integer, Entry>> logLines = new LinkedList<>();
	public static int logLineCount = 0;

	public static void push(Entry y)
	{
		logLines.addLast(new Tuple<>(logLineCount, y));
		logLineCount++;
		if (logLines.size() > VISIBLE_LOG_LINE_COUNT) logLines.removeFirst();
		System.out.println("[" + y.type + "] " + y.line);
	}

	//

	public static void main(String[] args) throws Exception
	{
		ProcessBuilder processBuilder = new ProcessBuilder(get("command"));
		processBuilder.directory(new File(get("directory")));
		Process process = processBuilder.start();

		new ThreadOutput("stdout", "black", process.getInputStream(), get("encoding")).start();
		new ThreadOutput("stderr", "red", process.getErrorStream(), get("encoding")).start();
		new ThreadInput("stdin", "green", process.getOutputStream(), get("encoding")).start();

		process.waitFor();

		System.exit(0);
	}

}
