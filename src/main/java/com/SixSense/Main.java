package com.SixSense;

import com.SixSense.data.commands.Operation;
import com.SixSense.data.logic.*;
import com.SixSense.engine.SessionEngine;
import com.SixSense.mocks.TestingMocks;
import com.SixSense.queue.WorkerQueue;
import com.SixSense.util.EncryptionUtils;
import com.SixSense.util.FileUtils;
import com.SixSense.util.MessageLiterals;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import javax.crypto.SecretKey;
import java.util.Scanner;
import java.util.concurrent.Future;

@SpringBootApplication
public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);

    //To run remotely, enter the following command in cli (parameter order matters!):
    //java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -jar /sixsense/OperatingSystem.jar
    //logger will output messages above INFO level to system.out
    public static void main(String[] args) {
        ConfigurableApplicationContext appContext = SpringApplication.run(Main.class, args);
        logger.info("SixSense Session engine demo");
        logger.info("Press any key to start tests");
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
        logger.info("Starting now");

        /*try {
            *//*for(int i=0; i<10; i++) {
                String testMessage = "The quick encrypted brown fox jumps over the lazy decrypted dog";
                logger.info("Original message: " + testMessage);

                SecretKey encryptionKey = EncryptionUtils.generateEncryptionKey();
                String encryptedMessage = EncryptionUtils.encryptString(testMessage, encryptionKey);
                logger.info("Encrypted message: " + encryptedMessage);

                String decryptedMessage = EncryptionUtils.decryptString(encryptedMessage, encryptionKey);
                logger.info("Decrypted message: " + decryptedMessage);
            }*//*

            String rootDir = MessageLiterals.projectDirectory();
            SecretKey encryptionKey = EncryptionUtils.generateEncryptionKey();
            EncryptionUtils.encryptFile(rootDir + "/lorem_large.txt", rootDir + "/lorem_encrypted", encryptionKey);
            logger.info("File has been encrypted");

            EncryptionUtils.decryptFile(rootDir + "/lorem_encrypted", rootDir + "/lorem_decrypted.txt", encryptionKey);
            logger.info("File has been decrypted");

            String originalHash = FileUtils.nonCryptoHash(rootDir + "/lorem_large.txt");
            String decryptedHash = FileUtils.nonCryptoHash(rootDir + "/lorem_decrypted.txt");
            logger.info("original hash = " + originalHash);
            logger.info("decrypted hash = " + decryptedHash);
        }catch (Exception e){
            logger.error(e);
        }*/

        try {
            SessionEngine engineInstance = (SessionEngine)appContext.getBean("sessionEngine");
            WorkerQueue queueInstance = (WorkerQueue)appContext.getBean("workerQueue");

            //Operation operation = TestingMocks.f5BigIpBackup("172.31.254.66", "root", "password");
            Operation operation = TestingMocks.f5BigIpBackup("172.31.252.179", "root", "qwe123");

            Future<ExpressionResult> backupResult = queueInstance.submit(() -> engineInstance.executeOperation(operation));
            logger.info("Operation " + operation.getFullOperationName() + " Completed with result " + backupResult.get().getOutcome());
            logger.info("Result Message: " + backupResult.get().getMessage());
        } catch (Exception e) {
            logger.error("A fatal exception was encountered - applications is closing now", e);
        }

        SpringApplication.exit(appContext);
    }
}
