package net.yapbam.data.xml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.zip.*;
import javax.crypto.*;
import javax.crypto.spec.*;

import net.yapbam.util.Base64Encoder;

public class BetterReader {
    private static final String ENCRYPTION_HEADER = "<Yapbam password encoded file 2.0>";

    private static final byte[] SALT = new byte[]{ (byte)0xc7, (byte)0x23, (byte)0xa5, (byte)0xfc, (byte)0x7e, (byte)0x38, (byte)0xee, (byte)0x09};
	private static final String ALGORITHM = "PBEWITHMD5ANDDES"; //$NON-NLS-1$
	private static final PBEParameterSpec PBE_PARAM_SPEC = new PBEParameterSpec(SALT, 16);

    public static void main(String[] args) {
        try {
            BetterReader reader = new BetterReader();
            try (BufferedReader input = new BufferedReader(new InputStreamReader(reader.openFile(new File("oneMoreWithGtiPwd.zip"), "gti")))) {
                input.lines().forEach(System.out::println);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public InputStream openFile(File file, String password) throws Exception {
        InputStream input = new BufferedInputStream(new FileInputStream(file));
        
        try {
            // 1. check if it's a zip file
            input.mark(2);
            int byte1 = input.read();
            int byte2 = input.read();
            input.reset();
            
            if (byte1 == 0x50 && byte2 == 0x4B) { // Magic bytes "PK" for ZIP
                // If it's a zip file, extract the first (and unique) entry
                ZipInputStream zis = new ZipInputStream(input);
                ZipEntry entry = zis.getNextEntry();
                if (entry == null) {
                    throw new IOException("ZIP file is empty");
                }
                input = zis; // Le contenu de l'entrée est maintenant lisible
            }
            
            // 2. check if it's encrypted
            input.mark(ENCRYPTION_HEADER.length());
            
            byte[] headerBytes = new byte[ENCRYPTION_HEADER.length()];
            int read = input.read(headerBytes);
            
            boolean isEncrypted = false;
            if (read == ENCRYPTION_HEADER.length()) {
                String header = new String(headerBytes, StandardCharsets.UTF_8);
                isEncrypted = ENCRYPTION_HEADER.equals(header);
            }
            
            if (isEncrypted) {
                if (password == null || password.isEmpty()) {
                    throw new AccessControlException("Password required for encrypted file");
                }
                
                // check password
                verifyPassword(input, password);
                
                // create decrypted stream
                input = createDecryptionStream(input, password);
            } else {
                // Not encrypted, reset to the beginning of the content
                input.reset();
            }
            
            return input;
            
        } catch (Exception e) {
            try { input.close(); } catch (IOException ignored) {}
            throw e;
        }
    }
    
    private InputStream createDecryptionStream(InputStream input, String password) throws Exception {
        Cipher cipher = getCipher(Cipher.DECRYPT_MODE, password, false);
        return new CipherInputStream(input, cipher);
    }
    
    private void verifyPassword(InputStream stream, String password) throws IOException, AccessControlException {
        byte[] digest = getDigest(password);
        byte[] fileDigest = new byte[digest.length];
        int missing = fileDigest.length;
        
        while (missing > 0) {
            int nb = stream.read(fileDigest, fileDigest.length - missing, missing);
            if (nb == -1) {
                throw new IOException("end of stream reached before end of password digest");
            }
            missing -= nb;
        }
        
        if (!MessageDigest.isEqual(digest, fileDigest)) {
            throw new AccessControlException("invalid password");
        }
    }

    	/** Creates a new cipher based on a password.
	 * @param mode The cipher mode (could be Cipher.ENCRYPT_MODE or Cipher.DECRYPT_MODE)
	 * @param compatibilityMode 
	 */
	private static Cipher getCipher(int mode, String password, boolean compatibilityMode) throws GeneralSecurityException {
		SecretKey pbeKey = getSecretKey(password, compatibilityMode);
		Cipher cipher = Cipher.getInstance(ALGORITHM);
		cipher.init(mode, pbeKey, PBE_PARAM_SPEC);
		return cipher;
	}
	
	/** Gets the secret key corresponding to a password.
	 * @param password A password
	 * @param compatibilityMode true to use an old yapbam style key (which was not compatible with Android)
	 * @return a Secret key
	 * @throws InvalidKeySpecException
	 * @throws NoSuchAlgorithmException
	 */
	@SuppressWarnings("deprecation")
	private static SecretKey getSecretKey(String password, boolean compatibilityMode) throws InvalidKeySpecException, NoSuchAlgorithmException {
        if (compatibilityMode) {
            return new net.yapbam.util.BinaryPBEKey(password.getBytes(StandardCharsets.UTF_8));
        } else {
            password = Base64Encoder.encode(password.getBytes(StandardCharsets.UTF_8));
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(new PBEKeySpec(password.toCharArray()));
        }
	}

    /** Gets the SHA digest of a password.
	 * @param password The password
	 * @return The password digest.
	 */
	private static byte[] getDigest(String password) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA");
			digest.update(SALT);
			return digest.digest(password.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}
}