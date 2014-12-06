package vace117.garage.opener.secure.channel.crypto;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;

import vace117.garage.opener.secure.channel.CommunicationChannel;
import vace117.garage.opener.secure.channel.Conversation;
import vace117.garage.opener.secure.channel.ConversationExpiredException;
import vace117.garage.opener.secure.channel.SecurityException;
import vace117.garage.opener.secure.channel.AbstractSecureChannelClient;

/**
 * Provides a secure request/response style communication with a server. 
 * <p>
 * The specifics of talking to the server are abstracted into <code>CommunicationChannel</code>.
 * <p>
 * This class handles all of the crypto, as well as secure conversation establishment,
 * which protects the server against replay attacks.
 * <p>
 * Calling <code>createConversation()</code> results in the following handshake:
 * <pre>
 * 		Client 1) encryptSendAndWaitForResponse("NEED_CHALLENGE")
 * 		Spark 1) Use PRNG to generate random Challenge[16]
 * 		Spark 2) Calculate sessionKey == HMAC(Key, Challenge[16])
 * 		Spark 3) Start 5 second timer
 * 		Spark 4) SparkResponse(Challenge[16])
 * 		Client 2) Calculate sessionToken = HMAC(Key, Challenge[16])
 * 		Client 3) Return a new instance of <code>SecureConversation(sessionToken)</code>
 * </pre>
 * <p>
 * After obtaining an instance of <code>SecureConversation</code>, users can send any 
 * messages they wish via <code>String SecureConversation.sendMessage(String)</code>
 *
 * @see AESChannelClient#encryptSendAndWaitForResponse(byte[]) encryptSendAndWaitForResponse() for details of how messages are encoded 
 * 
 * @author Val Blant
 */
public class AESChannelClient extends AbstractSecureChannelClient {
	
	private SecureRandom random = new SecureRandom();

	public AESChannelClient(CommunicationChannel commChannel) {
		super(commChannel);
		
		PRNGFixes.apply();
	}

	public Conversation createConversation() throws SecurityException, IOException {
		byte[] conversationNonce = encryptSendAndWaitForResponse("NEED_CHALLENGE".getBytes());
		
		return new AESSecuredConversation(this, new ConversationToken(conversationNonce));
	}
	
	/**
	 * Send a message as part of an established Conversation
	 * 
	 * @param message
	 * @param token
	 * @return
	 * @throws ConversationExpiredException
	 */
	String sendMessage(String message, ConversationToken token) throws ConversationExpiredException, SecurityException, IOException {
		// The format of the message is [conversationToken, MESSAGE]
		//
		ByteBuffer payload = ByteBuffer.allocate(token.getBytes().length + message.getBytes().length);
		payload.put(token.getBytes()).put(message.getBytes());
		
		byte[] plainTextResponse = encryptSendAndWaitForResponse(payload.array());
		
		String response = new String(plainTextResponse);
		
		if ( "SESSION_EXPIRED".equals(response) ) {
			throw new ConversationExpiredException();
		}

		return response;
	}
	
	/**
	 * Encrypts the plain text, sends it over the <code>CommunicationChannel</code>, waits to receive
	 * encrypted response, decrypts and returns the plain text payload.
	 * <p>
	 * The format of the outgoing message is as follows:
	 * <pre>
	 *   [Message_Length[2], IV_Send[16], AES_CBC(Master_Key, IV_Send, payloadToSend), <==== HMAC(Master_Key)]
	 * </pre>
	 * <p>
	 * The format of received messages is:
	 * <pre>
	 *   [Message_Length[2], IV_Response[16], AES_CBC(Master_Key, IV_Response, responsePayload), <==== HMAC(Master_Key)]
	 * </pre>
	 * 
	 * 
	 * @param payloadToSend
	 * @return
	 */
	private byte[] encryptSendAndWaitForResponse(byte[] payloadToSend) throws SecurityException, IOException {
		// Encrypt
		//
		byte[] cipherText = encryptData(payloadToSend);
		
		// Send cipher text
		//
		commChannel.write(cipherText);
		
		// The first 2 bytes of the response are the message length, so we read those first, and then get the rest
		// once we know the full length.
		//
		ByteBuffer msgLengthBuffer = ByteBuffer.wrap( commChannel.read(2) ).order(ByteOrder.LITTLE_ENDIAN);
		short responseLength = msgLengthBuffer.asShortBuffer().get(0);
		ByteBuffer restOfResponseBuffer = ByteBuffer.wrap( commChannel.read(responseLength - 2) );
		ByteBuffer fullResponseBuffer = ByteBuffer.allocate(responseLength);
		
		// Decrypt
		//
		byte[] encryptedData = fullResponseBuffer.put(msgLengthBuffer.array()).put(restOfResponseBuffer.array()).array();
		byte[] decryptedData = decryptData(encryptedData);
		
		return decryptedData;
	}
	
	/**
	 * @param encryptedData
	 * @return Plain text verified with HMAC and decrypted with AES 
	 */
	private byte[] decryptData(byte[] encryptedData) throws SecurityException {
		try {
			int dataLength = encryptedData.length;
			
			ByteBuffer dataBuffer = ByteBuffer.wrap(encryptedData);
			
			// Verify that locally computed HMAC matches the received one
			//
			byte[] hmacDataToVerify = new byte[dataLength - 20];
			byte[] receivedHmac = new byte[20];
			dataBuffer.get(hmacDataToVerify);
			dataBuffer.get(receivedHmac);
			
	        Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(MasterKey.getMasterKey());

            byte[] localHmac = mac.doFinal(hmacDataToVerify);
            
            if ( !Arrays.equals(receivedHmac, localHmac) ) {
            	throw new SecurityException();
            }
			
            // Grab the IV that was used to encrypt this data
        	//
            byte[] iv = new byte[16];
            dataBuffer.position(2); dataBuffer.get(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            
        	// Decrypt the message
        	//
            int cipherTextLength = hmacDataToVerify.length - dataBuffer.position();
            byte[] cipherText = new byte[cipherTextLength];
            dataBuffer.get(cipherText);
            
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
	        cipher.init(Cipher.DECRYPT_MODE, MasterKey.getMasterKey(), ivSpec);
	        byte[] plainText = cipher.doFinal(cipherText);
	        
			return plainText;
			
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("AES or HMAC algorithm problem detected", e);
		}
	}
	
	/**
	 * @param plainText
	 * @return AES-128 CBC encrypted, PKCS7 padded cipher text 
	 */
	private byte[] encryptData(byte[] plainText) {
		try {
			ByteBuffer sendData = ByteBuffer.allocate(180).order(ByteOrder.LITTLE_ENDIAN);
			sendData.position(2); // Leave room for data length prefix
			
			// Generate IV_Send[16]
			//
			IvParameterSpec sendIV = generateSendIV();
			sendData.put(sendIV.getIV());

			// Encrypt command
			//
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
	        cipher.init(Cipher.ENCRYPT_MODE, MasterKey.getMasterKey(), sendIV);
	        byte[] encrypted = cipher.doFinal(plainText);
	        sendData.put(encrypted);
	        
	        // Calculate total length
	        //
	        short dataLength = (short) (2 + sendIV.getIV().length + encrypted.length + 20); // HMAC is 20 bytes
	        sendData.putShort(0, dataLength);
	        
	        Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(MasterKey.getMasterKey());

            // Compute the HMAC
            //
            byte[] everythingSoFar = new byte[sendData.position()]; 
            sendData.rewind(); sendData.get(everythingSoFar);
            byte[] rawHmac = mac.doFinal(everythingSoFar);
            sendData.put(rawHmac);
	        
            // Data transmission is ready
            //
            byte[] result = new byte[sendData.position()];
            sendData.rewind(); sendData.get(result);
            
    		return result;
	        
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("AES or HMAC algorithm problem detected", e);
		}
		
	}
	
	private IvParameterSpec generateSendIV() {
		byte[] iv = new byte[16];
		random.nextBytes(iv);
		
//		return new IvParameterSpec(new byte[] {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1});
		return new IvParameterSpec(iv);
	}	
}
