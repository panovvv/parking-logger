package bleserial;

import tinyb.BluetoothDevice;
import tinyb.BluetoothGattCharacteristic;
import tinyb.BluetoothGattService;
import tinyb.BluetoothNotification;

import java.nio.CharBuffer;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by vadim on 8/4/17.
 */
public class BleSerial {

    private final static Logger LOGGER = Logger.getLogger(BleSerial.class.getName());

    private static final int INPUT_BUFFER_SIZE = 1024;

    private final BluetoothDevice bluetoothDevice;

    private final String uartServiceUuid;

    private BluetoothGattCharacteristic uartBufferCharacteristic;
    private final String uartCharacteristicUuid;
    private final int uartCharacteristicLength;

    private CharBuffer inputBuffer;
    private UARTReceiveNotification uartReceiveNotification;

    public BleSerial(BluetoothDevice bluetoothDevice, String uartServiceUuid,
                     String uartCharacteristicUuid, int uartCharacteristicLength) {
        this.bluetoothDevice = bluetoothDevice;
        this.uartServiceUuid = uartServiceUuid;
        this.uartCharacteristicUuid = uartCharacteristicUuid;
        this.uartCharacteristicLength = uartCharacteristicLength;
    }

    private static BluetoothGattService getService(BluetoothDevice device, String UUID) {
        LOGGER.log(Level.INFO, "Services exposed by device:");
        BluetoothGattService targetService = null;
        List<BluetoothGattService> services;
        do {
            services = device.getServices();
            if (Objects.nonNull(services) && !services.isEmpty()) {
                for (BluetoothGattService service : services) {
                    LOGGER.log(Level.INFO, "UUID: " + service.getUUID());
                    if (service.getUUID().equals(UUID))
                        targetService = service;
                }
            } else {
                LOGGER.log(Level.INFO, "No services found!");
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        } while (Objects.isNull(services));

        return targetService;
    }

    private static BluetoothGattCharacteristic getCharacteristic(BluetoothGattService service, String UUID) {
        List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
        if (characteristics == null)
            return null;

        for (BluetoothGattCharacteristic characteristic : characteristics) {
            if (characteristic.getUUID().equals(UUID))
                return characteristic;
        }
        return null;
    }

    public void connect() throws BleSerialException {
        if (bluetoothDevice.connect())
            LOGGER.log(Level.INFO, "BLE device connected");
        else {
            throw new BleSerialException("Could not connect device.");
        }
        initialize();
    }

    public void initialize() throws BleSerialException {
        BluetoothGattService uartService = getService(bluetoothDevice, uartServiceUuid);

        if (Objects.isNull(uartService)) {
            bluetoothDevice.disconnect();
            throw new BleSerialException("This device does not have the UART service we are looking for.");
        }
        LOGGER.log(Level.INFO, "Found the UART service: " + uartService.getUUID());

        uartBufferCharacteristic = getCharacteristic(uartService, uartCharacteristicUuid);

        if (Objects.isNull(uartBufferCharacteristic)) {
            bluetoothDevice.disconnect();
            throw new BleSerialException("Could not find the UART buffer characteristic.");
        }

        LOGGER.log(Level.INFO, "Found the UART buffer characteristic: " + uartCharacteristicUuid);

        inputBuffer = CharBuffer.allocate(INPUT_BUFFER_SIZE);
        uartReceiveNotification = new UARTReceiveNotification(inputBuffer);
        uartBufferCharacteristic.enableValueNotifications(uartReceiveNotification);
    }

    public boolean disconnect() {
        return bluetoothDevice.disconnect();
    }

    public boolean getConnected() {
        return bluetoothDevice.getConnected();
    }

    public void print(String s) throws InterruptedException {
        if (s.length() > uartCharacteristicLength) {
            for (String split : s.split(String.format("(?<=\\G.{%d})", uartCharacteristicLength))) {
                uartBufferCharacteristic.writeValue(split.getBytes());
            }
        } else {
            uartBufferCharacteristic.writeValue(s.getBytes());
        }
    }

    public void println(String s) throws InterruptedException {
        print(s.concat("\r\n"));
    }

    public void enableValueNotifications(BluetoothSerialNotification notification) {
        uartReceiveNotification.setNotification(notification);
    }

    public void disableValueNotifications() {
        uartReceiveNotification.setNotification(null);
    }

    public int available() {
        return inputBuffer.position();
    }

    public String read() {
        inputBuffer.flip();
        String result = inputBuffer.toString();
        inputBuffer.clear();
        return result;
    }

    public void enableConnectedNotifications(BluetoothNotification<Boolean> notification) {
        bluetoothDevice.enableConnectedNotifications(notification);
    }

    public void disableConnectedNotifications() {
        bluetoothDevice.disableConnectedNotifications();
    }

}
