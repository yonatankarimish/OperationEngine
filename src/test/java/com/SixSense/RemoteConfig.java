package com.SixSense;

import org.testng.annotations.DataProvider;


public class RemoteConfig {

    @DataProvider(name = "f5BigIpConfig")
    public Object[][] remoteDeviceConfiguration(){
        //TestNG requires data providers to return a two-dimensional array wrapping the map
        return new Object[][] {
            {"172.31.252.179", "root", "qwe123"}
        };
    }
}
