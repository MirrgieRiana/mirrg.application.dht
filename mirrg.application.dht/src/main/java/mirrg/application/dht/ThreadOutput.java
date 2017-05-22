package mirrg.application.dht;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

public class ThreadOutput extends Thread
{

	private BufferedReader in;
	private String color;

	public ThreadOutput(String name, String color, InputStream in, String encoding) throws UnsupportedEncodingException
	{
		super(name);
		this.color = color;
		this.in = new BufferedReader(new InputStreamReader(in, encoding));
	}

	@Override
	public void run()
	{
		try {
			String line;
			while ((line = in.readLine()) != null) {
				Main.push(new Entry(color, getName().toUpperCase(), line));
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
