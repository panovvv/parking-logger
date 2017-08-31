package bleserial;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import tinyb.BluetoothDevice;
import tinyb.BluetoothException;
import tinyb.BluetoothManager;

import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class BleDiscoveryService extends ScheduledService<ObservableList<BluetoothDeviceWrapper>> {

    private final static Logger LOGGER = Logger.getLogger(BleDiscoveryService.class.getName());

    private final BluetoothManager bluetoothManager;

    public BleDiscoveryService() {
        bluetoothManager = BluetoothManager.getBluetoothManager();
        boolean discoveryStarted = bluetoothManager.startDiscovery();
        LOGGER.log(Level.INFO, "Discovery started: " + (discoveryStarted ? "true" : "false"));
    }

    @Override
    public boolean cancel() {
        try {
            bluetoothManager.stopDiscovery();
            LOGGER.log(Level.INFO, "Discovery stopped.");
        } catch (BluetoothException e) {
            LOGGER.log(Level.SEVERE, "Cannot stop discovery!", e);
        }
        return super.cancel();
    }

    @Override
    protected Task<ObservableList<BluetoothDeviceWrapper>> createTask() {
        return new Task<ObservableList<BluetoothDeviceWrapper>>() {
            protected ObservableList<BluetoothDeviceWrapper> call() {
                if (isCancelled()) {
                    return FXCollections.emptyObservableList();
                }
                List<BluetoothDevice> list = bluetoothManager.getDevices();
                if (Objects.nonNull(list) && !list.isEmpty()) {
                    LOGGER.log(Level.INFO, "BLE SCAN RESULTS");
                    list.forEach(d -> LOGGER.log(Level.INFO, "Device found: " + d.getName()));
                    return FXCollections.observableArrayList(list.stream()
                            .map(BluetoothDeviceWrapper::new)
                            .collect(Collectors.toList()));
                } else {
                    return FXCollections.emptyObservableList();
                }
            }
        };
    }
}
