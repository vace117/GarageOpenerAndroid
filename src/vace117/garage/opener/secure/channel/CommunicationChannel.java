package vace117.garage.opener.secure.channel;

import java.io.IOException;

/**
 * Abstraction for a comm channel. This can be anything.
 *
 * @author Val Blant
 */
public interface CommunicationChannel {
	public void open() throws IOException;
	
	public void close() throws IOException;

	public byte[] read(int numberOfBytes) throws IOException;
	
	public void write(byte[] bytes) throws IOException;
}
