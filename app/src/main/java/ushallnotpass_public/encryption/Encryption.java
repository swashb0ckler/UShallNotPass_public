package ushallnotpass_public.encryption;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Encryption {


    private static final String encryptionAlgorithm = "AES/CBC/PKCS5Padding";

    public static IvParameterSpec generateIv() {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return new IvParameterSpec(iv);
    }

    public static SecretKey createSecretKey(String password, String salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), 64000, 256);
        SecretKey secret = new SecretKeySpec(factory.generateSecret(spec)
                .getEncoded(), "AES");
        return secret;
    }

    public static String encrypt(String input, SecretKey key,
                                 IvParameterSpec iv) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException {

        // Adding IV to string
        byte[] ivBytes = iv.getIV();
        String ivString = Base64.getEncoder().encodeToString(ivBytes);

        Cipher cipher = Cipher.getInstance(encryptionAlgorithm);
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        byte[] cipherText = cipher.doFinal(input.getBytes());
        return ivString + "|" + Base64.getEncoder()
                .encodeToString(cipherText);
    }

    public static String decrypt(String cipherText, SecretKey key) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException {


        Cipher cipher = Cipher.getInstance(encryptionAlgorithm);

        // Remaking IV into IvSpec
        String pattern = "^(.*?)\\|";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(cipherText);
        String iv = "";
        if (matcher.find()) {
            iv = matcher.group(1);
        }

        byte[] decodedIvBytes = Base64.getDecoder().decode(iv);
        IvParameterSpec decodedIv = new IvParameterSpec(decodedIvBytes);

        //Removing Iv from cipherText
        cipherText = cipherText.replaceAll(pattern, "");

        byte[] plainText = new byte[0];

        //try-catch to overwrite error message when using a wrong password
        try {
            cipher.init(Cipher.DECRYPT_MODE, key, decodedIv);
            plainText = cipher.doFinal(Base64.getDecoder()
                    .decode(cipherText));
        } catch (Exception e) {
            //
        }

        return new String(plainText);
    }


}
