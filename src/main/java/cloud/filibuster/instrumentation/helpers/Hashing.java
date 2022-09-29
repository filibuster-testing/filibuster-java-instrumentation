package cloud.filibuster.instrumentation.helpers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

/**
 * Hashing function for use when hashing call sites for use in execution indexes.
 */
// From: https://stackoverflow.com/questions/4895523/java-string-to-sha1
public class Hashing {
    private Hashing() {

    }

    /**
     * Given a string, produce a digest for use in JSON.
     *
     * @param inputString string to be converted to a digest
     * @return string or string as hex representation
     */
    public static String createDigest(String inputString) {
        try {
            MessageDigest crypt = MessageDigest.getInstance("SHA-1");
            crypt.reset();
            crypt.update(inputString.getBytes(StandardCharsets.UTF_8));
            return byteToHex(crypt.digest());
        } catch (NoSuchAlgorithmException e) {
            return inputString;
        }
    }

    /**
     * Convert byte array to hex.
     *
     * @param hash byte array of hash to convert to hex representation
     * @return hex representation
     */
    private static String byteToHex(byte[] hash)
    {
        Formatter formatter = new Formatter();
        for (byte b : hash) {
            formatter.format("%02x", b);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }
}
