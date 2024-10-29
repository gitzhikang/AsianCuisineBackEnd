package com.asiancuisine.asiancuisine.config;

import org.apache.ibatis.jdbc.Null;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.jcraft.jsch.Session;
import com.jcraft.jsch.JSch;

@Component
@Profile("dev")
public class SSHTunnelManager {

    private Session session;

    @Value("${ac.ssh.host}")
    public String sshHost;

    @Value("${ac.ssh.privateKeyPath}")
    private String privateKeyPath;

    private final int SESSION_TIMEOUT = 1000000;

    @PostConstruct
    public void openSshTunnel() {
        try {

            JSch jsch = new JSch();
            // config SSH uername、host、port
            session = jsch.getSession("lz238", sshHost,22);
            session.setConfig("StrictHostKeyChecking", "no");
            jsch.addIdentity(privateKeyPath);

            // build connection
            session.connect(SESSION_TIMEOUT);
            // map localhost:3307 to remoteHost:3306
            session.setPortForwardingL(3307, "localhost", 3306);

            System.out.println("SSH Tunnel established from localhost:3307 to localhost:3306");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @PreDestroy
    public void closeSshTunnel() {
        if (session != null && session.isConnected()) {
            session.disconnect();
            System.out.println("SSH Tunnel closed.");
        }
    }
}
