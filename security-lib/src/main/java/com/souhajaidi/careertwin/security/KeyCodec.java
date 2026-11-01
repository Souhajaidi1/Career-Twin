package com.souhajaidi.careertwin.security;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/** Reads and writes RSA keys as single-line base64 DER, which survives environment variables. */
public final class KeyCodec {

    private KeyCodec() {}

    public static PublicKey decodePublic(String base64Der) {
        try {
            byte[] der = Base64.getMimeDecoder().decode(base64Der.trim());
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            throw new IllegalStateException("careertwin.jwt.public-key is not a base64 X.509 RSA public key", ex);
        }
    }

    public static PrivateKey decodePrivate(String base64Der) {
        try {
            byte[] der = Base64.getMimeDecoder().decode(base64Der.trim());
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            throw new IllegalStateException("the private key is not a base64 PKCS#8 RSA private key", ex);
        }
    }

    /** A fresh 2048-bit pair as {@code [privateBase64, publicBase64]}. Used by tests and by scripts. */
    public static String[] generateBase64Pair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            return new String[] {
                Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded()),
                Base64.getEncoder().encodeToString(pair.getPublic().getEncoded())
            };
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("RSA is not available", ex);
        }
    }
}
