package io.disruptedsystems.libdtn.common.data.bundlev7;

import io.disruptedsystems.libdtn.common.data.eid.Eid;
import io.disruptedsystems.libdtn.common.data.security.CipherSuites;
import io.disruptedsystems.libdtn.common.data.security.SecurityContext;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Test class to test SecurityContext.
 * @author Lucien Loiseau on 06/11/18.
 */
public class SecurityContextTest {


    /**
     * Mock the SecurityContext to provide a single cipher, digest and mac.
     * @return a new SecurityContext.
     */
    public static SecurityContext mockSecurityContext() {
        return new SecurityContext() {

            String key = "0123456789abcdef";
            String initVector = "testInitVector--";

            @Override
            public MessageDigest initDigestForIntegrity(int cipherSuiteId, Eid securitySource)
                    throws NoSuchAlgorithmException {
                return CipherSuites.fromId(cipherSuiteId).getMessageDigest();
            }

            @Override
            public MessageDigest initDigestForVerification(int cipherSuiteId, Eid securitySource)
                    throws NoSuchAlgorithmException {
                return CipherSuites.fromId(cipherSuiteId).getMessageDigest();
            }

            @Override
            public Cipher initCipherForEncryption(int cipherSuiteId, Eid securitySource) throws
                    NoSecurityContextFound,
                    NoSuchAlgorithmException,
                    NoSuchPaddingException {
                SecretKeySpec skeySpec;
                IvParameterSpec ivParameterSpec;

                try {
                    skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
                    ivParameterSpec = new IvParameterSpec(initVector.getBytes());
                } catch (UnsupportedEncodingException e) {
                    throw new NoSecurityContextFound();
                }

                Cipher cipher;
                try {
                    cipher = CipherSuites.fromId(cipherSuiteId)
                            .getCipher();
                    cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivParameterSpec);
                } catch (InvalidKeyException | InvalidAlgorithmParameterException ike) {
                    ike.printStackTrace();
                    throw new NoSecurityContextFound();
                }
                return cipher;
            }

            @Override
            public Cipher initCipherForDecryption(int cipherSuiteId, Eid securitySource) throws
                    NoSecurityContextFound,
                    NoSuchAlgorithmException,
                    NoSuchPaddingException {
                SecretKeySpec skeySpec;
                IvParameterSpec ivParameterSpec;

                try {
                    skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
                    ivParameterSpec = new IvParameterSpec(initVector.getBytes());
                } catch (UnsupportedEncodingException e) {
                    throw new NoSecurityContextFound();
                }

                Cipher cipher;
                try {
                    cipher = CipherSuites.fromId(cipherSuiteId)
                            .getCipher();
                    cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivParameterSpec);
                } catch (InvalidKeyException | InvalidAlgorithmParameterException ike) {
                    throw new NoSecurityContextFound();
                }
                return cipher;
            }
        };

    }

}
