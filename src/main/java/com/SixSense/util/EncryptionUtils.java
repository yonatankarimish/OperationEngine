package com.SixSense.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.*;
import java.util.Base64;

public class EncryptionUtils {
    private static final Logger logger = LogManager.getLogger(EncryptionUtils.class);
    private static final byte IV_LENGTH = 16; //length of initialization vectors, in bytes; required length for AES-CTR
    private static final byte KEY_LENGTH = 16; //length of encryption keys, in bytes

    //Generate a random encryption key
    public static SecretKey generateEncryptionKey(){
        /*Initialize a secure pseudo-random number generator
         * and a 128 bit randomly generated AES encryption key*/
        byte[] randomBytes = new byte[KEY_LENGTH]; //16 bytes * 8 bits each
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(randomBytes);
        return new SecretKeySpec(randomBytes, "AES");
    }

    /*Encrypt a short string using AES-CTR 128 bit symmetric encryption (stream cipher)
    * Maximum string size that can be safely encrypted is around {FileUtils.localPartitionBlockSize}, which is a few KB at most*/
    public static String encryptString(String message, SecretKey encryptionKey) throws GeneralSecurityException {
        /*NEVER REUSE THE SAME INITIALIZATION VECTOR WITH THE SAME ENCRYPTION KEY!
         * Initialization vector (iv for short) can be public, and must be 16 bytes long
         * it can be used once, and only once.*/
        byte[] initializationVector = new byte[IV_LENGTH]; //16 bytes * 8 bits each
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(initializationVector);

        /*Encrypt the message using Counter-Mode stream cipher*/
        byte[] encryptedMessage;
        try {
            final Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
            IvParameterSpec paramSpec = new IvParameterSpec(initializationVector);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, paramSpec);
            encryptedMessage = cipher.doFinal(message.getBytes());
        } catch (NoSuchAlgorithmException e) {
            logger.error("Failed to encrypt message - The algorithm specified is invalid or does not exist", e);
            throw e;
        } catch (NoSuchPaddingException e) {
            logger.error("Failed to encrypt message - Running environment failed to provide padding mechanism", e);
            throw e;
        } catch (InvalidAlgorithmParameterException e) {
            logger.error("Failed to encrypt message - Wrong parameters passed to ParameterSpec", e);
            throw e;
        } catch (InvalidKeyException e) {
            logger.error("Failed to encrypt message - Encryption key is not valid", e);
            throw e;
        } catch (IllegalBlockSizeException e) {
            logger.error("Failed to encrypt message - Cipher block size is not valid", e);
            throw e;
        } catch (BadPaddingException e) {
            logger.error("Failed to encrypt message - message is not padded according to specified padding mechanism", e);
            throw e;
        }

        /*Generate the encrypted block, consisting of {iv-size|initialization-vector|encrypted-message}*/
        ByteBuffer messageBlock = ByteBuffer.allocate(4 + initializationVector.length + encryptedMessage.length);
        messageBlock.putInt(initializationVector.length);
        messageBlock.put(initializationVector);
        messageBlock.put(encryptedMessage);

        //return a base-64 string representing of the encrypted block
        return new String(Base64.getEncoder().encode(messageBlock.array()));
    }

    /*Encrypt file contents and write them to a new file, using AES-CTR 128 bit symmetric encryption (stream cipher)
     * Maximum file size that can be safely encrypted is around 64GB*/
    public static void encryptFile(String decryptedFileName, String encryptedFileName, SecretKey encryptionKey) throws GeneralSecurityException, IOException {
       /* NEVER REUSE THE SAME INITIALIZATION VECTOR WITH THE SAME ENCRYPTION KEY!
         * Initialization vector (iv for short) can be public, and must be 16 bytes long
         * it can be used once, and only once.*/
        byte[] initializationVector = new byte[IV_LENGTH]; //16 bytes * 8 bits each
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(initializationVector);

        //Create an encryption cipher using Counter-Mode stream cipher
        final Cipher cipher;
        try {
            cipher = Cipher.getInstance("AES/CTR/NoPadding");
            IvParameterSpec paramSpec = new IvParameterSpec(initializationVector);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, paramSpec);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Failed to encrypt file - The algorithm specified is invalid or does not exist", e);
            throw e;
        } catch (NoSuchPaddingException e) {
            logger.error("Failed to encrypt file - Running environment failed to provide padding mechanism", e);
            throw e;
        } catch (InvalidAlgorithmParameterException e) {
            logger.error("Failed to encrypt file - Wrong parameters passed to ParameterSpec", e);
            throw e;
        } catch (InvalidKeyException e) {
            logger.error("Failed to encrypt file - Encryption key is not valid", e);
            throw e;
        }

        //Stream the contents of the decrypted file and write them to the encrypted file
        FileChannel inChannel = null, outChannel = null;
        try{
            //Initialize I/O channels
            RandomAccessFile decryptedFile = new RandomAccessFile(decryptedFileName, "r");
            RandomAccessFile encryptedFile = new RandomAccessFile(encryptedFileName, "rw");
            inChannel = decryptedFile.getChannel();
            outChannel = encryptedFile.getChannel();

            final ByteBuffer inBuffer = ByteBuffer.allocate(FileUtils.localPartitionBlockSize * 16); //decrypted bytes
            final ByteBuffer outBuffer = ByteBuffer.allocate(FileUtils.localPartitionBlockSize * 16); //encrypted bytes
            int readBytes = 0;

            //Write initial bytes, consisting of {iv-size|initialization-vector|encrypted-message}
            outBuffer.putInt(initializationVector.length);
            outBuffer.put(initializationVector);
            outBuffer.flip();

            outChannel.write(outBuffer);
            outBuffer.clear();

            do {
                //Encrypt file contents and write them to encrypted file
                readBytes = inChannel.read(inBuffer);
                inBuffer.flip();

                if(readBytes > 0) {
                    byte[] encryptedBytes = cipher.update(inBuffer.array(), 0, readBytes);
                    outBuffer.put(encryptedBytes);
                    outBuffer.flip();

                    outChannel.write(outBuffer);
                    outBuffer.clear();
                }
                inBuffer.clear();
            } while (readBytes != -1);

            //finalize the encryption and write the remaining encrypted bytes
            byte[] encryptedBytes = cipher.doFinal();
            outBuffer.put(encryptedBytes);
            outBuffer.flip();
            outChannel.write(outBuffer);
            outBuffer.clear();
        } catch (BadPaddingException e) {
            logger.error("Failed to encrypt file - block is not padded according to specified padding mechanism", e);
            throw e;
        }  catch (FileNotFoundException e){
            logger.error("Failed to write to encrypted file - File was not found", e);
            throw e;
        } catch (IOException e){
            logger.error("Failed to write to encrypted file - read/write operation encountered an error", e);
            throw e;
        } finally {
            FileUtils.finalizeCloseableResource(inChannel, outChannel);
        }
    }

    //Decrypt a short string previously encrypted using AES-CTR 128 bit symmetric encryption (stream cipher)
    public static String decryptString(String encrypted, SecretKey encryptionKey) throws GeneralSecurityException {
        /*Convert encrypted message to byte buffer*/
        byte[] decodedBytes = Base64.getDecoder().decode(encrypted.getBytes());
        ByteBuffer messageBlock = ByteBuffer.wrap(decodedBytes);

        //Perform integrity checks on initialization vector and extract it from the message block
        int actualIVSize = messageBlock.getInt();
        if(actualIVSize != IV_LENGTH) { // check input parameter
            throw new IllegalArgumentException("Failed to decrypt message - initialization vector size is not valid");
        }

        byte[] initializationVector = new byte[actualIVSize];
        messageBlock.get(initializationVector);

        //Extract the encrypted message from the message block
        byte[] encryptedMessage = new byte[messageBlock.remaining()];
        messageBlock.get(encryptedMessage);

        /*Decrypt the message using Counter-Mode stream cipher*/
        byte[] decryptedMessage;
        try {
            final Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
            IvParameterSpec paramSpec = new IvParameterSpec(initializationVector);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, paramSpec);
            decryptedMessage = cipher.doFinal(encryptedMessage);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Failed to decrypt message - The algorithm specified is invalid or does not exist", e);
            throw e;
        } catch (NoSuchPaddingException e) {
            logger.error("Failed to decrypt message - Running environment failed to provide padding mechanism", e);
            throw e;
        } catch (InvalidAlgorithmParameterException e) {
            logger.error("Failed to decrypt message - Wrong parameters passed to ParameterSpec", e);
            throw e;
        } catch (InvalidKeyException e) {
            logger.error("Failed to decrypt message - Encryption key is not valid", e);
            throw e;
        } catch (IllegalBlockSizeException e) {
            logger.error("Failed to decrypt message - Cipher block size is not valid", e);
            throw e;
        } catch (BadPaddingException e) {
            logger.error("Failed to decrypt message - message is not padded according to specified padding mechanism", e);
            throw e;
        }

        //return a plain text string representing the decrypted block
        return new String(decryptedMessage);
    }

    //Decrypt file contents and write them to a new file, using AES-CTR 128 bit symmetric encryption (stream cipher)
    public static void decryptFile(String encryptedFileName, String decryptedFileName, SecretKey encryptionKey) throws GeneralSecurityException, IOException {
        //Initialize I/O channels
        FileChannel inChannel = null, outChannel = null;
        try {
            RandomAccessFile encryptedFile = new RandomAccessFile(encryptedFileName, "r");
            RandomAccessFile decryptedFile = new RandomAccessFile(decryptedFileName, "rw");
            inChannel = encryptedFile.getChannel();
            outChannel = decryptedFile.getChannel();
        } catch (FileNotFoundException e){
            logger.error("Failed to write to decrypted file - File was not found", e);
            throw e;
        }

        Cipher cipher = null;
        final ByteBuffer inBuffer = ByteBuffer.allocate(FileUtils.localPartitionBlockSize * 16); //encrypted bytes
        final ByteBuffer outBuffer = ByteBuffer.allocate(FileUtils.localPartitionBlockSize * 16); //decrypted bytes
        int initialReadBytes = 0;
        int readBytes = 0;

        try {
            do {
                readBytes = inChannel.read(inBuffer);
                if (readBytes > 0) {
                    //If a cipher is not yet initialized, attempt to extract initialization vector and create one
                    if (cipher == null) {
                        initialReadBytes += readBytes;
                        if (initialReadBytes >= 4 + IV_LENGTH) {
                            //Perform integrity checks on initialization vector and extract it from the incoming byte buffer
                            inBuffer.flip();
                            int actualIVSize = inBuffer.getInt();
                            if (actualIVSize != IV_LENGTH) { // check input parameter
                                throw new IllegalArgumentException("Failed to decrypt message - initialization vector size is not valid");
                            }

                            byte[] initializationVector = new byte[actualIVSize];
                            inBuffer.get(initializationVector);

                            //Create an encryption cipher using Counter-Mode stream cipher
                            cipher = Cipher.getInstance("AES/CTR/NoPadding");
                            IvParameterSpec paramSpec = new IvParameterSpec(initializationVector);
                            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, paramSpec);

                            //Decrypt any remaining bytes from the current read operation and write them to decrypted file
                            if(inBuffer.remaining() > 0) {
                                outBuffer.put(cipher.update(inBuffer.array(), 4 + IV_LENGTH, inBuffer.remaining()));
                                outBuffer.flip();

                                outChannel.write(outBuffer);
                                outBuffer.clear();
                            }

                            inBuffer.clear();
                        }
                    } else {
                        //Decrypt file contents and write them to decrypted file
                        inBuffer.flip();
                        outBuffer.put(cipher.update(inBuffer.array(), 0, readBytes));
                        outBuffer.flip();

                        outChannel.write(outBuffer);
                        outBuffer.clear();
                        inBuffer.clear();
                    }
                }

            } while (readBytes != -1);

            //finalize the decryption and write the remaining decrypted bytes
            byte[] decryptedBytes = cipher.doFinal();
            outBuffer.put(decryptedBytes);
            outBuffer.flip();
            outChannel.write(outBuffer);
            outBuffer.clear();
        } catch (NoSuchAlgorithmException e) {
            logger.error("Failed to decrypt file - The algorithm specified is invalid or does not exist", e);
            throw e;
        } catch (NoSuchPaddingException e) {
            logger.error("Failed to decrypt file - Running environment failed to provide padding mechanism", e);
            throw e;
        } catch (InvalidAlgorithmParameterException e) {
            logger.error("Failed to decrypt file - Wrong parameters passed to ParameterSpec", e);
            throw e;
        } catch (InvalidKeyException e) {
            logger.error("Failed to decrypt file - Encryption key is not valid", e);
            throw e;
        }  catch (IOException e) {
            logger.error("Failed to write to decrypt file - read/write operation encountered an error", e);
            throw e;
        } catch (BadPaddingException e) {
            logger.error("Failed to decrypt file - block is not padded according to specified padding mechanism", e);
            throw e;
        } finally {
            FileUtils.finalizeCloseableResource(inChannel, outChannel);
        }
    }
}
