package com.SixSense.util;

import net.jpountz.lz4.*;
import net.jpountz.xxhash.StreamingXXHash64;
import net.jpountz.xxhash.XXHashFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtils {
    private static final Logger logger = LogManager.getLogger(FileUtils.class);
    private static int localPartitionBlockSize;

    //obtain block size for local installation partition to optimize buffered reads
    static{
        LocalShell localShell = new LocalShell();
        try {
            LocalShellResult result = localShell.runCommand("blockdev --getbsz " + MessageLiterals.localPartitionName);
            if(result.getExitCode() == 0){
                localPartitionBlockSize = Integer.valueOf(result.getOutput().get(0));
            }else{
                throw new Exception("Failed to obtain block size from local shell. Caused by: " + result.getErrors().get(0));
            }
        } catch (Exception e) {
            logger.error("Failed to obtain block size for local partition name", e);
        }
    }

    /*performance: 1GB = 1 second
    * for a detailed explanation about hash functions in general, please see https://dadario.com.br/cryptographic-and-non-cryptographic-hash-functions/
    * in short, non-cryptographic hash functions are much faster, but should not be used when the contents of the file should be kept secret */
    public static String nonCryptoHash(String filename) throws IOException {
        //Create pointer and iterator for the provided file, and obtain file channel
        RandomAccessFile file = new RandomAccessFile(filename, "r");
        FileChannel inChannel = file.getChannel();

        /*buffer size will be a compromise between speed and memory:
        * larger buffer size will mean the current call will run faster, as each access will fill in more bytes
        * larger buffer size will mean less concurrent calls can run before JVM memory is reached*/
        ByteBuffer buffer = ByteBuffer.allocate(localPartitionBlockSize*16);

        //Initialize hashing algorithm
        int seed = 0x9747b28c; // used to initialize the hash value; can be any arbitrary value, but must be the same value always.
        XXHashFactory hashFactory = XXHashFactory.fastestInstance();
        StreamingXXHash64 streamingHash = hashFactory.newStreamingHash64(seed);

        //Read the file in chunks of bytes, to avoid storing file contents in memory
        int read = 0;
        do{
            read = inChannel.read(buffer);
            buffer.flip();

            if(read > 0) {
                streamingHash.update(buffer.array(), 0, read); //perform next hashing step with next block of bytes
            }
            buffer.clear();
        }while(read != -1);

        //Convert to hexadecimal representation and return hash value
        return Long.toHexString(streamingHash.getValue());
    }

    public static void compress(String rawFileName) throws IOException{
        compress(rawFileName, rawFileName+".lz4");
    }


    //working implementations lz4
    /*TODO: although faster than gzip implementation, these are 20 times slower than the claimed 800MB per second compression claimed by lz4.
    * opened issue with lz4-java at https://github.com/lz4/lz4-java/issues/100, check for their answer once available*/
    public static void compress(String decompressedFileName, String compressedFileName) throws IOException{
        InputStream directIn = null;
        BufferedInputStream bufferedIn = null;
        OutputStream directOut = null;
        BufferedOutputStream bufferedOut = null;
        LZ4BlockOutputStream outStream = null;

        try {
            Path compressedPath = Paths.get(compressedFileName);
            if(Files.exists(compressedPath)){
                Files.createFile(compressedPath);
            }

            directIn = Files.newInputStream(Paths.get(decompressedFileName));
            bufferedIn = new BufferedInputStream(directIn);
            directOut = Files.newOutputStream(compressedPath);
            bufferedOut = new BufferedOutputStream(directOut);
            outStream = new LZ4BlockOutputStream(bufferedOut);

            final byte[] buffer = new byte[localPartitionBlockSize * 16];
            int readBytes = 0;
            do {
                readBytes = bufferedIn.read(buffer);
                if(readBytes > 0) {
                    outStream.write(buffer, 0, readBytes);
                }
            } while (readBytes != -1);
        }finally {
            if(outStream != null) {
                outStream.close();
            }
            if(bufferedOut != null) {
                bufferedOut.close();
            }
            if(directOut != null) {
                directOut.close();
            }
            if(bufferedIn != null) {
                bufferedIn.close();
            }
            if(directIn != null) {
                directIn.close();
            }
        }
    }

    public static void decompress(String compressedFileName, String decompressedFileName) throws IOException{
        InputStream directIn = null;
        BufferedInputStream bufferedIn = null;
        OutputStream directOut = null;
        BufferedOutputStream bufferedOut = null;
        LZ4BlockInputStream inStream = null;

        try {
            Path decompressedPath = Paths.get(decompressedFileName);
            if(Files.exists(decompressedPath)){
                Files.createFile(decompressedPath);
            }
            directIn = Files.newInputStream(Paths.get(compressedFileName));
            bufferedIn = new BufferedInputStream(directIn);
            inStream = new LZ4BlockInputStream(bufferedIn);
            directOut = Files.newOutputStream(decompressedPath);
            bufferedOut = new BufferedOutputStream(directOut);

            final byte[] buffer = new byte[localPartitionBlockSize * 16];
            int readBytes = 0;
            do {
                readBytes = inStream.read(buffer);
                if(readBytes > 0) {
                    bufferedOut.write(buffer, 0, readBytes);
                }
            } while (readBytes != -1);
        }finally {
            if(bufferedOut != null) {
                bufferedOut.close();
            }
            if(directOut != null) {
                directOut.close();
            }
            if(inStream != null) {
                inStream.close();
            }
            if(bufferedIn != null) {
                bufferedIn.close();
            }
            if(directIn != null) {
                directIn.close();
            }
        }
    }

    //not-yet working implementations
   /* public static void compress(String decompressedFileName, String compressedFileName) throws IOException{
        LZ4Factory factory = LZ4Factory.fastestInstance();
        LZ4CompressorWithLength compressor = new LZ4CompressorWithLength(factory.fastCompressor());
        RandomAccessFile decompressedFile = new RandomAccessFile(decompressedFileName, "r");
        RandomAccessFile compressedFile = new RandomAccessFile(compressedFileName, "rw");
        FileChannel inChannel = null;
        FileChannel outChannel = null;

        try {
            inChannel = decompressedFile.getChannel();
            outChannel = compressedFile.getChannel();

            final ByteBuffer inBuffer = ByteBuffer.allocate(localPartitionBlockSize * 16);
            final ByteBuffer outBuffer = ByteBuffer.allocate(localPartitionBlockSize * 16);
            int readBytes = 0;
            do {
                readBytes = inChannel.read(inBuffer);
                inBuffer.flip();

                if(readBytes > 0) {
                    outBuffer.put(compressor.compress(inBuffer.array(), 0, inBuffer.position()+1));
                    outBuffer.flip();

                    outChannel.write(outBuffer);
                    outBuffer.clear();
                }
                inBuffer.clear();
            } while (readBytes != -1);
        }finally {
            if(inChannel != null) {
                inChannel.close();
            }
            if(outChannel != null) {
                outChannel.close();
            }
        }
    }

    public static void decompress(String compressedFileName, String decompressedFileName) throws IOException{
        LZ4Factory factory = LZ4Factory.fastestInstance();
        LZ4DecompressorWithLength decompressor = new LZ4DecompressorWithLength(factory.fastDecompressor());
        RandomAccessFile compressedFile = new RandomAccessFile(compressedFileName, "r");
        RandomAccessFile decompressedFile = new RandomAccessFile(decompressedFileName, "rw");
        FileChannel inChannel = null;
        FileChannel outChannel = null;

        try {
            inChannel = compressedFile.getChannel();
            outChannel = decompressedFile.getChannel();

            final ByteBuffer inBuffer = ByteBuffer.allocate(localPartitionBlockSize * 16);
            final ByteBuffer outBuffer = ByteBuffer.allocate(localPartitionBlockSize * 16);
            int readBytes = 0;
            do {
                readBytes = inChannel.read(inBuffer);
                inBuffer.flip();

                if(readBytes > 0) {
                    outBuffer.put(decompressor.decompress(inBuffer.array()));
                    outBuffer.flip();

                    outChannel.write(outBuffer);
                    outBuffer.clear();
                }
                inBuffer.clear();
            } while (readBytes != -1);
        }finally {
            if(inChannel != null) {
                inChannel.close();
            }
            if(outChannel != null) {
                outChannel.close();
            }
        }
    }*/
}
