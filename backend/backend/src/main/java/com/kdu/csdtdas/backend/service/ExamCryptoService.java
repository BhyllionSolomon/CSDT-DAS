package com.kdu.csdtdas.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

/** Envelope encryption: a random AES-256 data key encrypts each paper; the server master key wraps that data key. */
@Service
public class ExamCryptoService {
    private static final int NONCE_BYTES=12, TAG_BITS=128;
    private final SecretKey masterKey;
    private final SecureRandom random=new SecureRandom();
    public ExamCryptoService(@Value("${app.exam-master-key:}") String encoded) {
        if(encoded==null || encoded.isBlank()) { masterKey=null; return; }
        byte[] raw=Base64.getDecoder().decode(encoded.trim());
        if(raw.length!=32) throw new IllegalStateException("APP_EXAM_MASTER_KEY must be a Base64-encoded 32-byte key.");
        masterKey=new SecretKeySpec(raw,"AES");
    }
    public boolean configured(){return masterKey!=null;}
    public Encrypted encrypt(byte[] plain) {
        requireKey();
        try {
            KeyGenerator generator=KeyGenerator.getInstance("AES"); generator.init(256);
            SecretKey dataKey=generator.generateKey();
            byte[] fileNonce=nonce();
            Cipher fileCipher=Cipher.getInstance("AES/GCM/NoPadding");
            fileCipher.init(Cipher.ENCRYPT_MODE,dataKey,new GCMParameterSpec(TAG_BITS,fileNonce));
            byte[] ciphertext=fileCipher.doFinal(plain);
            byte[] wrappedNonce=nonce();
            Cipher wrap=Cipher.getInstance("AES/GCM/NoPadding");
            wrap.init(Cipher.ENCRYPT_MODE,masterKey,new GCMParameterSpec(TAG_BITS,wrappedNonce));
            byte[] wrapped=wrap.doFinal(dataKey.getEncoded());
            return new Encrypted(join(fileNonce,ciphertext),Base64.getEncoder().encodeToString(join(wrappedNonce,wrapped)));
        } catch(Exception e){throw new IllegalStateException("Examination paper encryption failed.",e);}
    }
    public byte[] decrypt(byte[] ciphertext,String wrappedKey) {
        requireKey();
        try {
            byte[] w=Base64.getDecoder().decode(wrappedKey);
            byte[] wn=java.util.Arrays.copyOfRange(w,0,NONCE_BYTES);
            byte[] wc=java.util.Arrays.copyOfRange(w,NONCE_BYTES,w.length);
            Cipher unwrap=Cipher.getInstance("AES/GCM/NoPadding");
            unwrap.init(Cipher.DECRYPT_MODE,masterKey,new GCMParameterSpec(TAG_BITS,wn));
            SecretKey dataKey=new SecretKeySpec(unwrap.doFinal(wc),"AES");
            byte[] fn=java.util.Arrays.copyOfRange(ciphertext,0,NONCE_BYTES);
            byte[] fc=java.util.Arrays.copyOfRange(ciphertext,NONCE_BYTES,ciphertext.length);
            Cipher decrypt=Cipher.getInstance("AES/GCM/NoPadding");
            decrypt.init(Cipher.DECRYPT_MODE,dataKey,new GCMParameterSpec(TAG_BITS,fn));
            return decrypt.doFinal(fc);
        } catch(Exception e){throw new IllegalStateException("Examination paper decryption failed. The key or paper may be invalid.",e);}
    }
    private byte[] nonce(){byte[] n=new byte[NONCE_BYTES];random.nextBytes(n);return n;}
    private byte[] join(byte[] a,byte[] b){return ByteBuffer.allocate(a.length+b.length).put(a).put(b).array();}
    private void requireKey(){if(masterKey==null)throw new IllegalStateException("Exam encryption is not configured. Set APP_EXAM_MASTER_KEY before accepting papers.");}
    public record Encrypted(byte[] ciphertext,String wrappedKey){}
}