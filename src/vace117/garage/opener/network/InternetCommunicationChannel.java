package vace117.garage.opener.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;

import vace117.garage.opener.crypto.CommunicationChannel;

/**
 * Communicates with Spark over the Internet via a Socket.
 *
 * @author Val Blant
 */
public class InternetCommunicationChannel implements CommunicationChannel {
	
	private static final int MAX_MESSAGE_SIZE = 256; 
	private byte[] data_buffer = new byte[MAX_MESSAGE_SIZE];

	private Socket socket;
	private OutputStream outToServer;
	private InputStream inFromServer;
	
	private boolean isOpen = false;
	
	private InetSocketAddress sparkCore = new InetSocketAddress("192.168.7.121", 6666);
	private static final int CONNECT_TIMEOUT = 5000; //ms
	private static final int READ_TIMEOUT = 5000; //ms. Allows the app to throw an error if the connection is lost.
	
	

	@Override
	public void open() throws IOException {
		if ( !isOpen ) {
			socket = new Socket();
			socket.setSoTimeout(READ_TIMEOUT);
			socket.connect(sparkCore, CONNECT_TIMEOUT);
			
			outToServer = socket.getOutputStream();
			inFromServer = socket.getInputStream();
			
			isOpen = true;
		}
	}

	@Override
	public void close() throws IOException {
		if ( isOpen ) {
			outToServer.close();
			inFromServer.close();
			socket.close();
			
			isOpen = false;
		}
	}

	@Override
	public byte[] read(int numberOfBytes) throws IOException {
		if ( socket.isConnected() && !socket.isInputShutdown() && !socket.isOutputShutdown() ) {
			inFromServer.read(data_buffer, 0, numberOfBytes);
			
			return Arrays.copyOfRange(data_buffer, 0, numberOfBytes);
		}
		else {
			throw new IOException("Lost connection to the garage!");
		}
	}

	@Override
	public void write(byte[] bytes) throws IOException {
		if ( socket.isConnected() && !socket.isInputShutdown() && !socket.isOutputShutdown() ) {
			outToServer.write(bytes);
		}
		else {
			throw new IOException("Lost connection to the garage!");
		}
	}

}
