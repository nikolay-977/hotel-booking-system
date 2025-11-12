package com.example.hotelservice.util;

import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.util.Base64;

public class JwtKeyGenerator {
    public static void main(String[] args) {
        SecretKey key = Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS256);
        String base64Key = Base64.getEncoder().encodeToString(key.getEncoded());

        System.out.println("Generated JWT Secret Key:");
        System.out.println(base64Key);
        System.out.println("Key length: " + base64Key.length() * 8 + " bits");
    }
}