package com.asiancuisine.asiancuisine.config;

import org.apache.ibatis.jdbc.Null;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.jcraft.jsch.Session;
import com.jcraft.jsch.JSch;

import java.io.FileInputStream;
import java.util.Properties;

@Component
@Profile("dev")
public class SSHTunnelManager {

    private Session session;

    @Value("${ac.ssh.host}")
    public String sshHost;

    @Value("${ac.ssh.privateKeyPath}")
    private String privateKeyPath;  // 私钥文件路径

//    @Value("${ac.ssh.passphrase}")
    private String passphrase = null;  // 私钥密码（如果有）

    private final int SESSION_TIMEOUT = 1000000;

    @PostConstruct
    public void openSshTunnel() {
        try {

            JSch jsch = new JSch();
            // 设置 SSH 连接的用户名、主机地址、端口
            session = jsch.getSession("lz238", sshHost,22);
            session.setConfig("StrictHostKeyChecking", "no");
            if (passphrase != null && !passphrase.isEmpty()) {
                jsch.addIdentity(privateKeyPath, passphrase);  // 如果私钥有密码
            } else {
                jsch.addIdentity(privateKeyPath);  // 无密码私钥
            }

            // 建立 SSH 连接
            session.connect(SESSION_TIMEOUT);

            // 将本地端口 localPort 映射到远程服务器的 remoteHost:remotePort
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
