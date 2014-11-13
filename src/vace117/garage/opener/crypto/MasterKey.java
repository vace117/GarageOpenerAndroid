package vace117.garage.opener.crypto;

import java.io.IOException;
import java.io.InputStream;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import vace117.garage.opener.GarageControlActivity;

public class MasterKey {
	
	final static byte MASTER_KEY[] = new byte[16];
	static {
		InputStream input = null;
		try {
			input = GarageControlActivity.getAssetManager().open("master.key");
			input.read(MASTER_KEY, 0, 16);

		} catch (Exception e) {
			throw new IllegalStateException("Couldn't read the Master Key file", e);
		}
		finally {
			try {
				if ( input != null) input.close();
			} catch (IOException e) {
				throw new IllegalStateException("Couldn't read the Master Key file", e);
			}
		}
	}
	
	static SecretKey getMasterKey() {
		return new SecretKeySpec(MASTER_KEY, "AES");
//		return new SecretKeySpec(new byte[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,1}, "AES");
	}

}
