package com.example.antiaddictionapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import androidx.core.app.NotificationCompat;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocalVpnService extends VpnService {

    private static final String TAG = "LocalVpnService";
    private static final String CLEANBROWSING_DNS = "185.228.168.168";
    
    private ParcelFileDescriptor vpnInterface;
    private ExecutorService executorService;
    private boolean isRunning = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, "VPN_CHANNEL_ID")
                .setContentTitle("AntiAddiction Protection")
                .setContentText("CleanBrowsing DNS is permanently active.")
                .setSmallIcon(android.R.drawable.ic_secure)
                .setOngoing(true)
                .build();
        startForeground(1, notification);

        if (!isRunning) {
            isRunning = true;
            executorService = Executors.newFixedThreadPool(2);
            setupVpn();
        }
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    "VPN_CHANNEL_ID",
                    "VPN Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private void setupVpn() {
        Builder builder = new Builder();
        builder.addAddress("10.0.0.2", 32);
        
        // We only want to intercept DNS traffic, which is typically routed to the DNS servers
        // configured on the system. For simplicity, we intercept ALL traffic, but only process DNS.
        builder.addRoute("0.0.0.0", 0); 
        builder.addDnsServer(CLEANBROWSING_DNS);
        
        try {
            builder.addDisallowedApplication(getPackageName());
        } catch (Exception e) {
            Log.e(TAG, "Failed to exclude app", e);
        }

        try {
            vpnInterface = builder.establish();
            Log.i(TAG, "VPN interface established");
            startPacketProxy();
        } catch (Exception e) {
            Log.e(TAG, "Failed to establish VPN", e);
        }
    }

    private void startPacketProxy() {
        executorService.execute(() -> {
            try (FileInputStream in = new FileInputStream(vpnInterface.getFileDescriptor());
                 FileOutputStream out = new FileOutputStream(vpnInterface.getFileDescriptor())) {

                byte[] buffer = new byte[32767];
                while (isRunning) {
                    int length = in.read(buffer);
                    if (length > 0) {
                        processPacket(buffer, length, out);
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "VPN thread stopped", e);
            }
        });
    }

    private void processPacket(byte[] packet, int length, FileOutputStream out) {
        // Basic IPv4 Parsing (First 20 bytes minimum)
        byte versionAndIhl = packet[0];
        int ihl = (versionAndIhl & 0x0F) * 4;
        byte protocol = packet[9];

        // Only process UDP packets
        if (protocol == 17) {
            int sourcePort = ((packet[ihl] & 0xFF) << 8) | (packet[ihl + 1] & 0xFF);
            int destPort = ((packet[ihl + 2] & 0xFF) << 8) | (packet[ihl + 3] & 0xFF);

            // If it's a DNS request
            if (destPort == 53) {
                handleDnsRequest(packet, length, ihl, sourcePort, out);
            }
        }
    }

    private void handleDnsRequest(byte[] packet, int length, int ihl, int sourcePort, FileOutputStream out) {
        // Extract DNS Payload
        int udpHeaderLen = 8;
        int payloadLen = length - ihl - udpHeaderLen;
        byte[] dnsPayload = new byte[payloadLen];
        System.arraycopy(packet, ihl + udpHeaderLen, dnsPayload, 0, payloadLen);

        executorService.execute(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                protect(socket); // CRITICAL: Protect socket from going through the VPN
                
                InetAddress dnsServer = InetAddress.getByName(CLEANBROWSING_DNS);
                DatagramPacket request = new DatagramPacket(dnsPayload, dnsPayload.length, dnsServer, 53);
                socket.send(request);

                byte[] responseBuffer = new byte[4096];
                DatagramPacket response = new DatagramPacket(responseBuffer, responseBuffer.length);
                socket.receive(response);

                // Reconstruct IP and UDP headers and write back to VPN
                byte[] responsePacket = buildResponsePacket(packet, ihl, response.getData(), response.getLength());
                synchronized (out) {
                    out.write(responsePacket);
                }
            } catch (IOException e) {
                Log.e(TAG, "DNS Proxy error", e);
            }
        });
    }

    private byte[] buildResponsePacket(byte[] originalRequest, int ihl, byte[] payload, int payloadLen) {
        int totalLength = ihl + 8 + payloadLen;
        byte[] response = new byte[totalLength];
        
        // Copy IP Header
        System.arraycopy(originalRequest, 0, response, 0, ihl);
        
        // Update Total Length in IP Header
        response[2] = (byte) (totalLength >> 8);
        response[3] = (byte) totalLength;
        
        // Swap Source and Dest IPs
        System.arraycopy(originalRequest, 12, response, 16, 4); // Src -> Dest
        System.arraycopy(originalRequest, 16, response, 12, 4); // Dest -> Src
        
        // IP Checksum (simplified - set to 0 for now, though Android might drop it if strict)
        response[10] = 0;
        response[11] = 0;
        long ipChecksum = calculateChecksum(response, 0, ihl);
        response[10] = (byte) (ipChecksum >> 8);
        response[11] = (byte) ipChecksum;

        // Copy UDP Header
        System.arraycopy(originalRequest, ihl, response, ihl, 8);
        
        // Swap UDP Ports
        System.arraycopy(originalRequest, ihl, response, ihl + 2, 2); // SrcPort -> DestPort
        System.arraycopy(originalRequest, ihl + 2, response, ihl, 2); // DestPort -> SrcPort
        
        // Update UDP Length
        int udpLen = 8 + payloadLen;
        response[ihl + 4] = (byte) (udpLen >> 8);
        response[ihl + 5] = (byte) udpLen;
        
        // Disable UDP Checksum (0 means no checksum)
        response[ihl + 6] = 0;
        response[ihl + 7] = 0;

        // Copy Payload
        System.arraycopy(payload, 0, response, ihl + 8, payloadLen);
        
        return response;
    }

    private long calculateChecksum(byte[] buf, int offset, int length) {
        long sum = 0;
        for (int i = 0; i < length; i += 2) {
            sum += ((buf[offset + i] & 0xFF) << 8) | (buf[offset + i + 1] & 0xFF);
        }
        while ((sum >> 16) > 0) {
            sum = (sum & 0xFFFF) + (sum >> 16);
        }
        return ~sum & 0xFFFF;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        if (executorService != null) {
            executorService.shutdownNow();
        }
        if (vpnInterface != null) {
            try {
                vpnInterface.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to close VPN interface", e);
            }
            vpnInterface = null;
        }
    }
}
