package bleserial;

import tinyb.BluetoothDevice;

public class BluetoothDeviceWrapper {

    private final BluetoothDevice device;

    public BluetoothDeviceWrapper(BluetoothDevice device) {
        this.device = device;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    @Override
    public String toString() {
        return device.getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BluetoothDeviceWrapper that = (BluetoothDeviceWrapper) o;

        return device != null ? device.equals(that.device) : that.device == null;
    }

    @Override
    public int hashCode() {
        return device != null ? device.hashCode() : 0;
    }
}
