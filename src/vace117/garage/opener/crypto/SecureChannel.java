package vace117.garage.opener.crypto;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;



public class SecureChannel {
	
	SecureRandom random = new SecureRandom();

	public SecureChannel() {
		PRNGFixes.apply();
		
		sendCommand("NEED_CHALLENGE");
	}
	
	public String sendCommand(String command) {
		String response = null;
		
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
	        byte[] encrypted = cipher.doFinal(command.getBytes());
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
            
	        System.out.println(result);
	        
		} catch (Exception e) {
			throw new IllegalStateException("Could not initialize AES cipher", e);
		}
		
		
		return response;
	}
	
	private IvParameterSpec generateSendIV() {
		byte[] iv = new byte[16];
		random.nextBytes(iv);
		
//		return new IvParameterSpec(new byte[] {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1});
		return new IvParameterSpec(iv);
	}
	
}
