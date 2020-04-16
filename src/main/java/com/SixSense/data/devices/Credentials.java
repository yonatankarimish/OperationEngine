package com.SixSense.data.devices;

import com.SixSense.data.IDeepCloneable;

public class Credentials implements IDeepCloneable<Credentials> {
    private String host;
    private String username;
    private transient String password;
    private int port;

    /*Try not to pollute with additional constructors
     * The empty constructor is for using the 'with' design pattern; Defaults to localhost ssh connections
     * The parameterized constructor is for complete constructors - where all arguments are known */
    public Credentials() {
        this.host = "localhost";
        this.username = "root";
        this.password = "password";
        this.port = 22;
    }

    public Credentials(String host, String username, String password, int port) {
        this.host = host;
        this.username = username;
        this.password = password;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Credentials withHost(String host) {
        this.host = host;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Credentials withUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Credentials withPassword(String password) {
        this.password = password;
        return this;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Credentials withPort(int port) {
        this.port = port;
        return this;
    }

    //Returns a new instance of the same credentials in its pristine state. That is - as if the new state was never executed
    @Override
    public Credentials deepClone(){
        return new Credentials()
            .withHost(this.host)
            .withUsername(this.username)
            .withPassword(this.password)
            .withPort(this.port);
    }

    @Override
    public String toString() {
        return "Credentials{" +
            "host='" + host + '\'' +
            ", username='" + username + '\'' +
            ", password='" + password + '\'' +
            ", port=" + port +
            '}';
    }
}
