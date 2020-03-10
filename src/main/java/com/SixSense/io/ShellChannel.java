package com.SixSense.io;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.TransportException;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ShellChannel implements Closeable {
    private final String name; //Identifying name for current channel
    private final net.schmizz.sshj.connection.channel.direct.Session shellSession; //SSH session that generates the shell process
    private final Session.Shell shell; //The operating system process to which we perform I/O

    private final BufferedWriter channelInput; //Buffered writer through which to write commands to shell input stream
    private final ProcessStreamWrapper channelOutputWrapper; //Runs in a separate thread with one purpose: clear the output stream all the time and keep the responses coming in
    private final List<String> channelOutput; //Line separated response (which we read) from both the shell output and error streams.
    private boolean isUnderDebug = false;
    private boolean isClosed = false;

    /*Shell Channels have a single constructor
    * Sessions write (and flush) directly to shell channels , and read the output from the channelOutput argument they supply
    * We assume the ssh client supplied here is already instantiated and connected to our localhost address
    *
    * Pseudo-terminals (PTY) do not allocate separate channels for output and errors.
     *Therefore, we only listen to the shell output stream, as the errors will be written there as well*/
    public ShellChannel(String name, SSHClient connectedSSHClient, com.SixSense.io.Session engineSession) throws ConnectionException, TransportException {
        this.name = name;
        this.shellSession = connectedSSHClient.startSession();
        this.shellSession.allocateDefaultPTY();
        this.shell = this.shellSession.startShell();
        this.channelOutput = new ArrayList<>();

        this.channelInput = new BufferedWriter(new OutputStreamWriter(this.shell.getOutputStream()));
        this.channelOutputWrapper = new ProcessStreamWrapper(this.shell.getInputStream(), engineSession, channelOutput);
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

    public boolean isUnderDebug() {
        return isUnderDebug;
    }

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
        this.shell.close();
        this.shellSession.close();
        this.channelInput.close();
        this.isClosed = true;
    }
}
