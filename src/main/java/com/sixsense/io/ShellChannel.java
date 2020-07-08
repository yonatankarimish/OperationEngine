package com.sixsense.io;

import com.sixsense.config.HostConfig;
import com.sixsense.model.logging.IDebuggable;
import com.sixsense.model.logging.Loggers;
import net.schmizz.sshj.DefaultConfig;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.SessionChannel;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.UserAuthException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.*;

public class ShellChannel implements Closeable, IDebuggable {
    private static final Logger sessionLogger = LogManager.getLogger(Loggers.SessionLogger.name());

    //Engine session related parameters
    private final String name; //Identifying name for current channel
    private final com.sixsense.io.Session engineSession; //parent session (engine session)
    private static final Object clientLock = new Object();

    //SSH connection related classes
    private static final DefaultConfig sshConfig = new DefaultConfig(); //Default configuration for ssh client
    private final SSHClient sshClient; //Wrapper around SSH client process, forked from the engine process
    private final SessionChannel sshChannel; //Implements the Session and Shell interfaces;  exposes an I/O api to the shell via a pseudo-terminal

    //I/O classes
    private final BufferedWriter channelInput; //Buffered writer through which to write commands to shell input stream
    private final ProcessStreamWrapper channelOutputWrapper; //Runs in a separate thread with one purpose: clear the output stream all the time and keep the responses coming in
    private final List<String> channelOutput; //Line separated response (which we read) from both the shell output and error streams.

    //State indicators
    private boolean isUnderDebug = false;
    private boolean isClosed = false;

    /*Shell Channels have a single constructor
    * Channels create an ssh client, which starts a session and allocate a pseudo-terminal (PTY) to the client
    * The shell exposes two streams: an input stream to which we write commands, and an output stream which we read from using a wrapper class (PSW)
    * Sessions write (and flush) directly to the input stream , and the output is read by the PSW into the ChannelOutput list
    *
    * Pseudo-terminals (PTY) do not allocate separate channels for output and errors.
     *Therefore, we only listen to the shell output stream, as the errors will be written there as well*/
    public ShellChannel(String name, HostConfig.Host localhostConfig, com.sixsense.io.Session engineSession) throws IOException {
        this.name = name;
        this.engineSession = engineSession;

        try {

            try {
                this.sshClient = new SSHClient(sshConfig); //1) Create an ssh client
                sshClient.addHostKeyVerifier(new PromiscuousVerifier());
                synchronized (clientLock) {
                    sshClient.connect(localhostConfig.getHost(), localhostConfig.getPort()); //2) connect to the local operating system (self-connection)
                    sshClient.authPassword(localhostConfig.getUsername(), localhostConfig.getPassword());  //3) using the credentials from localhostConfig
                }
            } catch (UserAuthException e) {
                throw new IOException("Failed to authenticate local SSH connection while creating a new SSH client. Cauesed by: ", e);
            } catch (TransportException e) {
                throw new IOException("Transport error experienced on SSH connection while creating a new SSH client. Cauesed by: ", e);
            } catch (IOException e) {
                throw new IOException("Failed to open local SSH connection while creating a new SSH client. Cauesed by: ", e);
            }

            try {
                Session session = this.sshClient.startSession(); //4) start an ssh channel using the ssh client
                session.allocateDefaultPTY(); // 5)Allocate a pseudo-terminal to the ssh session (https://linux.die.net/man/7/pty)
                session.startShell(); // 6)Start the shell by connecting to the allocated pty
                this.sshChannel = (SessionChannel)session; // 7) and cast the channel to the implementing subclass to expose it's full api
            } catch (IOException e) {
                throw new IOException("Failed to start a new SSH session from the generated SSH client. Caused by: ", e);
            }

        }catch (IOException e){
            //Ensure any partially opened resources are closed
            this.close();
            throw e;
        }

        this.channelOutput = new ArrayList<>();
        this.channelInput = new BufferedWriter(new OutputStreamWriter(this.sshChannel.getOutputStream()));
        this.channelOutputWrapper = new ProcessStreamWrapper(this.sshChannel.getInputStream(), engineSession, channelOutput);
    }

    //In order for the shell to process your input as a command written by a user, it should end with a line break character.
    public void write(String input) throws IOException {
        this.channelInput.write(input);
    }

    //Once you have written enough data to the shell channel, flush it so the input reaches the shell process
    public void flush() throws IOException {
        this.channelInput.flush();
    }

    public String getName() {
        return name;
    }

    public BufferedWriter getChannelInput() {
        return channelInput;
    }

    public ProcessStreamWrapper getChannelOutputWrapper() {
        return channelOutputWrapper;
    }

    public List<String> getChannelOutput() {
        return channelOutput;
    }

    @Override
    public boolean isUnderDebug() {
        return isUnderDebug;
    }

    @Override
    public void activateDebugMode() {
        this.channelOutputWrapper.activateDebugMode();
        isUnderDebug = true;
    }

    public boolean isClosed() {
        return isClosed;
    }

    //The only field by which shell channels are compared is the name field
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }else if (other == null || getClass() != other.getClass()) {
            return false;
        }else {
            ShellChannel otherAsChannel = (ShellChannel) other;
            return Objects.equals(name, otherAsChannel.name);
        }
    }

    //Used for distinguishing between shell channels in hash tables (sets, maps etc...)
    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public void close() throws IOException {
        /*SSH channel need to close before the streams to avoid reading data after marking the streams with "End-of-File"
         *(probably an implementation bug in sshj, but the natural order causes a ConnectionException to be thrown...) */
        boolean sshChannelClosed = closeChannelResource("sshChannel", this.sshChannel);
        boolean inputClosed = closeChannelResource("channelInput", this.channelInput);
        boolean outputClosed = closeChannelResource("channelOutputWrapper", this.channelOutputWrapper);
        boolean sshClientClosed = closeChannelResource("sshClient", this.sshClient);

        /*Inlining the close statements (partialClosure = close(A) && close(B)...)
         *will not close resources after the first "false" value (i.e. if close(A) returns false, close(B) won't be invoked)
         *so we close them all before checking for a partial closure*/
        boolean partialClosure = !(inputClosed && outputClosed && sshChannelClosed && sshClientClosed);
        this.isClosed = true; //Even if resources are left open, we do not want to invoke close() again on already-closed resources
        if(partialClosure){
            throw new IOException("Channel " + this.name + " for session " +  engineSession.getShortSessionId() + " failed to close one or more of it's resources");
        }
    }

    private boolean closeChannelResource(String resourceName, Closeable resource){
        boolean resourceClosed = true;
        if (resource != null) {
            try {
                resource.close();
            } catch (IOException e) {
                sessionLogger.error("Channel " + this.name + " of session " + engineSession.getShortSessionId() + " failed to close " + resourceName + ". Caused by: " + e.getMessage());
                resourceClosed = false;
            }
        }

        return resourceClosed;
    }
}
