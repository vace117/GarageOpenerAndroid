package vace117.garage.opener.crypto;

import java.io.IOException;

/**
 * 
 *
 * @author Val Blant
 */
public interface CommunicationChannel {
	public void open() throws IOException;
	
	public void close() throws IOException;

	public byte[] read(int numberOfBytes) throws IOException;
	
	public void write(byte[] bytes) throws IOException;
}
