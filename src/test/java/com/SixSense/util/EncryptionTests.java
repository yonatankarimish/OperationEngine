package com.SixSense.util;

import com.SixSense.SixSenseBaseTest;
import com.SixSense.data.BlockTests;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;
import java.util.Arrays;

@Test(groups = {"util"})
public class EncryptionTests extends SixSenseBaseTest {
    private static final Logger logger = LogManager.getLogger(BlockTests.class);

    public void testStringEncryption(){
        SecretKey encryptionKey = EncryptionUtils.generateEncryptionKey();

        try {
            String testMessage = "The quick encrypted brown fox jumps over the lazy decrypted dog";
            String encryptedMessage = EncryptionUtils.encryptString(testMessage, encryptionKey);
            String decryptedMessage = EncryptionUtils.decryptString(encryptedMessage, encryptionKey);

            logger.info("test message: " + testMessage);
            logger.info("decrypted message: " + decryptedMessage);
            Assert.assertEquals(testMessage, decryptedMessage);
        }catch (GeneralSecurityException e){
            logger.error(e);
            Assert.fail(e.getMessage());
        }
    }

    public void differentKeysDifferentEncryption(){
        SecretKey encryptionKeyA = EncryptionUtils.generateEncryptionKey();
        SecretKey encryptionKeyB = EncryptionUtils.generateEncryptionKey();

        logger.info("first encryption key bytes: " + Arrays.toString(encryptionKeyA.getEncoded()));
        logger.info("second encryption key bytes: " + Arrays.toString(encryptionKeyB.getEncoded()));
        Assert.assertNotEquals(encryptionKeyA.getEncoded(), encryptionKeyB.getEncoded());

        try {
            String testMessage = "The quick encrypted brown fox jumps over the lazy decrypted dog";
            String encryptedMessageA = EncryptionUtils.encryptString(testMessage, encryptionKeyA);
            String encryptedMessageB = EncryptionUtils.encryptString(testMessage, encryptionKeyB);

            logger.info("first encrypted message: " + encryptedMessageA);
            logger.info("second encrypted message: " + encryptedMessageB);
            Assert.assertNotEquals(encryptedMessageA, encryptedMessageB);
        }catch (GeneralSecurityException e){
            logger.error(e);
            Assert.fail(e.getMessage());
        }
    }

    public void sameKeyDifferentEncryption(){
        SecretKey encryptionKey = EncryptionUtils.generateEncryptionKey();

        try {
            String testMessage = "The quick encrypted brown fox jumps over the lazy decrypted dog";
            String encryptedMessageA = EncryptionUtils.encryptString(testMessage, encryptionKey);
            String encryptedMessageB = EncryptionUtils.encryptString(testMessage, encryptionKey);

            logger.info("first encrypted message: " + encryptedMessageA);
            logger.info("second encrypted message: " + encryptedMessageB);
            Assert.assertNotEquals(encryptedMessageA, encryptedMessageB);
        }catch (GeneralSecurityException e){
            logger.error(e);
            Assert.fail(e.getMessage());
        }
    }
}
