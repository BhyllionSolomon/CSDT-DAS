package com.kdu.csdtdas.backend.service;
import org.springframework.beans.factory.annotation.Autowired;import org.springframework.beans.factory.annotation.Value;import org.springframework.stereotype.Service;
import javax.crypto.Cipher;import javax.crypto.KeyGenerator;import javax.crypto.SecretKey;import javax.crypto.spec.GCMParameterSpec;import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;import java.security.SecureRandom;import java.util.Base64;

@Service
public class ExamCryptoService {
 private static final int NONCE_BYTES=12,TAG_BITS=128;private final SecretKey masterKey,previousMasterKey;private final SecureRandom random=new SecureRandom();
 @Autowired public ExamCryptoService(@Value("${app.exam-master-key:}") String active,@Value("${app.exam-previous-master-key:}") String previous){masterKey=decode(active);previousMasterKey=decode(previous);}
 public ExamCryptoService(String active){this(active,"");}
 private SecretKey decode(String encoded){if(encoded==null||encoded.isBlank())return null;byte[] raw=Base64.getDecoder().decode(encoded.trim());if(raw.length!=32)throw new IllegalStateException("Exam master keys must be Base64-encoded 32-byte AES keys.");return new SecretKeySpec(raw,"AES");}
 public boolean configured(){return masterKey!=null;}
 public Encrypted encrypt(byte[] plain){requireKey();try{KeyGenerator generator=KeyGenerator.getInstance("AES");generator.init(256);SecretKey dataKey=generator.generateKey();byte[] fileNonce=nonce();Cipher file=Cipher.getInstance("AES/GCM/NoPadding");file.init(Cipher.ENCRYPT_MODE,dataKey,new GCMParameterSpec(TAG_BITS,fileNonce));byte[] ciphertext=file.doFinal(plain);return new Encrypted(join(fileNonce,ciphertext),wrap(dataKey,masterKey));}catch(Exception e){throw new IllegalStateException("Examination paper encryption failed.",e);}}
 public byte[] decrypt(byte[] ciphertext,String wrappedKey){requireKey();try{SecretKey dataKey=unwrap(wrappedKey);byte[] fileNonce=java.util.Arrays.copyOfRange(ciphertext,0,NONCE_BYTES);byte[] fileCiphertext=java.util.Arrays.copyOfRange(ciphertext,NONCE_BYTES,ciphertext.length);Cipher file=Cipher.getInstance("AES/GCM/NoPadding");file.init(Cipher.DECRYPT_MODE,dataKey,new GCMParameterSpec(TAG_BITS,fileNonce));return file.doFinal(fileCiphertext);}catch(Exception e){throw new IllegalStateException("Examination paper decryption failed. The key or paper may be invalid.",e);}}
 public String rewrap(String wrappedKey){requireKey();try{return wrap(unwrap(wrappedKey),masterKey);}catch(Exception e){throw new IllegalStateException("Could not rotate an examination paper data key.",e);}}
 private SecretKey unwrap(String encoded)throws Exception{byte[] w=Base64.getDecoder().decode(encoded);byte[] n=java.util.Arrays.copyOfRange(w,0,NONCE_BYTES),c=java.util.Arrays.copyOfRange(w,NONCE_BYTES,w.length);Exception failure=null;for(SecretKey candidate:new SecretKey[]{masterKey,previousMasterKey}){if(candidate==null)continue;try{Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,candidate,new GCMParameterSpec(TAG_BITS,n));return new SecretKeySpec(cipher.doFinal(c),"AES");}catch(Exception e){failure=e;}}throw new IllegalStateException("No configured master key can unwrap this paper.",failure);}
 private String wrap(SecretKey key,SecretKey wrappingKey)throws Exception{byte[] n=nonce();Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,wrappingKey,new GCMParameterSpec(TAG_BITS,n));return Base64.getEncoder().encodeToString(join(n,c.doFinal(key.getEncoded())));}
 private byte[] nonce(){byte[] n=new byte[NONCE_BYTES];random.nextBytes(n);return n;}private byte[] join(byte[] a,byte[] b){return ByteBuffer.allocate(a.length+b.length).put(a).put(b).array();}
 private void requireKey(){if(masterKey==null)throw new IllegalStateException("Exam encryption is not configured. Set APP_EXAM_MASTER_KEY before accepting papers.");}
 public record Encrypted(byte[] ciphertext,String wrappedKey){}
}