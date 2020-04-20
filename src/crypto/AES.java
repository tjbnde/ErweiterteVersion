package crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

// The AES Standard allows only block sizes of 128 Bits
public class AES {

    private final static String VECTOR = "1122334455667788"; // 16 chars -> Corresponds to a byte Array with the size 16
    private final static String KEY = "VerteilteSysteme"; // 16 chars -> Must be the same length as the block size(=128 Bits/16 Bytes)
    private static Cipher encryption;
    private static Cipher decryption;


    // static initializer will run the first time the class is referenced, so the class attributes are
    // initialized properly
    static {
        try {
            // AES will only encrypt 128 Bit of data, so we need to choose a block mode if we want to encrypt whole messages
            // In CBC(=Cipher Block Chaining) each plaintext block is XORed with the previous ciphertext block
            // before being encrypted. That's why a initialization vector is needed
            IvParameterSpec initVector = new IvParameterSpec(VECTOR.getBytes());

            // Create the secret key based on the string "KEY"
            byte[] bKey = KEY.getBytes();
            SecretKey sKey = new SecretKeySpec(bKey, "AES");

            // Generate the cipher for encryption
            encryption = Cipher.getInstance("AES/CBC/PKCS5Padding");
            encryption.init(Cipher.ENCRYPT_MODE, sKey, initVector);

            // Generate the the cipher for decryption
            decryption = Cipher.getInstance("AES/CBC/PKCS5Padding");
            decryption.init(Cipher.DECRYPT_MODE, sKey, initVector);
        } catch (Exception e) {
            System.err.println("Generating ciphers failed!: " + e.getMessage());
        }
    }

    /**
     * Encrypt the message
     *
     * @param msg The message to encrypt
     * @return The encrypted message as String
     * @throws Exception The block sized used is illegal | Wrong key is used and therefore no valid padding
     */
    public static String encrypt(String msg) throws Exception {
        byte[] encrypted = encryption.doFinal(msg.getBytes());
        return Base64.getEncoder().encodeToString(encrypted);
    }

    /**
     * Decrypt the message
     *
     * @param msg The message to decrypt
     * @return The decrypted message as String
     * @throws Exception The block sized used is illegal | Wrong key is used and therefore no valid padding
     */
    public static String decrypt(String msg) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(msg);
        return new String(decryption.doFinal(bytes));
    }




}
