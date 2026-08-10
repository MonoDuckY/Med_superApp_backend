package com.yourproject.backend.audit;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;

@Component
@Getter
public class BackendInstanceIdentity {
    private final String instanceId;
    private final String hostName;
    private final String ipAddress;

    public BackendInstanceIdentity(
            @Value("${app.audit.instance-id-file:.backend-instance-id}") String instanceIdFile) {
        this.instanceId = loadOrCreateInstanceId(Path.of(instanceIdFile));
        this.hostName = resolveHostName();
        this.ipAddress = resolveIpAddress();
    }

    private String loadOrCreateInstanceId(Path path) {
        try {
            if (Files.exists(path)) {
                String existing = Files.readString(path).trim();
                if (!existing.isEmpty()) return existing;
            }
            String generated = UUID.randomUUID().toString();
            try {
                Files.writeString(path, generated, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                return generated;
            } catch (java.nio.file.FileAlreadyExistsException exception) {
                return Files.readString(path).trim();
            }
        } catch (IOException exception) {
            return UUID.nameUUIDFromBytes((resolveHostName() + "|" + resolveIpAddress()).getBytes()).toString();
        }
    }

    private String resolveHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception exception) {
            return "unknown-host";
        }
    }

    private String resolveIpAddress() {
        try {
            for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual()) continue;
                for (InetAddress address : Collections.list(networkInterface.getInetAddresses())) {
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                        return address.getHostAddress();
                    }
                }
            }
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception exception) {
            return "unknown-ip";
        }
    }
}
