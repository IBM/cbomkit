import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class JavaCryptoInModule {

    public void test() throws IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException {
        Cipher c = Cipher.getInstance("RSA/ECB/NoPadding");
        c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(MERCHANT_KEY.getBytes(), "AES"));
    }

}