package bleserial;

/**
 * Created by vadim on 8/4/17.
 */
public class BleSerialException extends Exception {

    public BleSerialException() {
    }

    public BleSerialException(String s) {
        super(s);
    }

    public BleSerialException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public BleSerialException(Throwable throwable) {
        super(throwable);
    }

    public BleSerialException(String s, Throwable throwable, boolean b, boolean b1) {
        super(s, throwable, b, b1);
    }

}
