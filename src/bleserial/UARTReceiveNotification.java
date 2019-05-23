package bleserial;

import tinyb.BluetoothNotification;

import java.nio.CharBuffer;
import java.util.Objects;

public class UARTReceiveNotification implements BluetoothNotification<byte[]> {

    private CharBuffer inputBuffer;
    private BluetoothSerialNotification notification;

    public UARTReceiveNotification(CharBuffer inputBuffer) {
        this.inputBuffer = inputBuffer;
        inputBuffer.clear();
    }

    @Override
    public void run(byte[] bytes) {
        for (byte b : bytes) {
            if (inputBuffer.hasRemaining()) {
                inputBuffer.put(byteToChar(b));
            }
        }
        if (Objects.nonNull(notification)) {
            notification.run(convertBytesToString(bytes));
        }
    }

    private String convertBytesToString(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            builder.append(byteToChar(b));
        }
        return builder.toString();
    }

    public void setNotification(BluetoothSerialNotification notification) {
        this.notification = notification;
    }

    private char byteToChar(byte b) {
        return (char) (b & 0xFF);
    }
}
