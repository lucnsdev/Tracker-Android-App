package lucns.tracker.mqtt.internal.messages;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class MqttConnectMessage extends MqttMessage {

    private final String clientId;
    private boolean isCleanSession;
    private final int keepAliveInterval;
    private final int mqttVersion;

    public MqttConnectMessage(byte info, byte[] data) throws IOException {
        super(MESSAGE_TYPE_CONNECT);
        this.mqttVersion = 4;
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bais);
        String protocol_name = decodeUTF8(dis);
        int protocol_version = dis.readByte();
        byte connect_flags = dis.readByte();
        keepAliveInterval = dis.readUnsignedShort();
        clientId = decodeUTF8(dis);
        dis.close();
    }

    public MqttConnectMessage(String clientId, boolean isCleanSession, int keepAliveInterval) {
        super(MESSAGE_TYPE_CONNECT);
        this.clientId = clientId;
        this.isCleanSession = isCleanSession;
        this.keepAliveInterval = keepAliveInterval;
        this.mqttVersion = 4;
    }

    protected byte getMessageInfo() {
        return (byte) 0;
    }

    public boolean isCleanSession() {
        return isCleanSession;
    }

    protected byte[] getVariableHeader() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        encodeUTF8(dos, "MQTT");
        dos.write(mqttVersion);

        byte connectFlags = 0;
        if (isCleanSession) {
            connectFlags |= 0x02;
        }
        dos.write(connectFlags);
        dos.writeShort(keepAliveInterval);
        dos.flush();
        return baos.toByteArray();
    }

    public byte[] getPayload() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        encodeUTF8(dos, clientId);
        dos.flush();
        return baos.toByteArray();
    }
}
