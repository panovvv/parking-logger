package gui;

import bleserial.BleDiscoveryService;
import bleserial.BleSerial;
import bleserial.BleSerialException;
import bleserial.BluetoothDeviceWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import tinyb.BluetoothDevice;
import webcam.CameraSettingsProvider;
import webcam.Utils;
import webcam.VideoLogger;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Controller {

    private final static Logger LOGGER = Logger.getLogger(Controller.class.getName());

    @FXML
    private Button connectBleButton;

    @FXML
    private Button connectCameraButton;

    @FXML
    private CheckBox refreshBleCheckbox;

    @FXML
    private Button refreshCamerasButton;

    @FXML
    private Button startLoggingButton;

    @FXML
    private ImageView webcamView;

    @FXML
    private ComboBox<BluetoothDeviceWrapper> bleDropdown;

    @FXML
    private ComboBox<Integer> cameraDropdown;

    @FXML
    private Label bleStatusLabel;

    @FXML
    private Label cameraStatusLabel;

    @FXML
    private Label loggingStatusLabel;

    // Constants
    private static final String UART_SERVICE_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb";
    private static final String UART_CHARACTERISTIC_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb";
    private static final int UART_CHARACTERISTIC_LENGTH = 20;

    // BLE-related variables
    private BleSerial bleSerial;
    private boolean bleSerialConnected = false;
    private BleDiscoveryService bleDiscoveryService;

    // Webcam-related variables
    private VideoCapture videoCapture = new VideoCapture();
    private boolean cameraConnected = false;
    private CameraSettingsProvider cameraSettings;
    private ScheduledExecutorService frameGrabber;
    Mat lastFrame = new Mat();

    // Logging-related variables
    private VideoLogger logger;
    private boolean logging;

    @FXML
    protected void startLoggingButtonClicked(ActionEvent actionEvent) {
        if (logging) {
            // stop logging
            setLoggingUi(false);
            setCameraConnectedUi(false);
            stopVideoCapture();
            logger.stopLogging();
        } else {
            // start logging
            if (cameraConnected && bleSerialConnected) {
                setLoggingUi(true);
                logger = new VideoLogger(videoCapture, cameraSettings);
                logger.startLogging();
                bleSerial.enableValueNotifications(s -> logger.pushNewEntry(s));
            }
        }
    }

    @FXML
    public void connectBleButtonClicked(ActionEvent actionEvent) throws BleSerialException {
        if (bleSerialConnected) {
            // disconnect
            bleSerial.disconnect();
            setBleSerialConnectedUi(false);
        } else {
            // connect
            BluetoothDeviceWrapper selected = bleDropdown.getSelectionModel().getSelectedItem();
            if (Objects.nonNull(selected)) {
                makeBleSerial(selected.getDevice(), !selected.getDevice().getConnected());
            }
        }
    }

    @FXML
    public void refreshCamerasButtonClicked(ActionEvent actionEvent) {
        int numberOfDevices = 0;
        VideoCapture capture = new VideoCapture();
        boolean opened;
        do {
            capture.open(numberOfDevices);
            opened = capture.isOpened();
            capture.release();
            if (opened) {
                numberOfDevices++;
            }
        } while (opened);

        List<Integer> cameraIndexes = IntStream.range(0, numberOfDevices)
                .boxed()
                .collect(Collectors.toList());
        cameraDropdown.setItems(FXCollections.observableArrayList(cameraIndexes));
        if (!cameraIndexes.isEmpty()) {
            cameraDropdown.getSelectionModel().selectFirst();
        }

    }

    @FXML
    public void connectCameraButtonClicked(ActionEvent actionEvent) {
        if (cameraConnected) {
            // disconnect
            setCameraConnectedUi(false);
            stopVideoCapture();
        } else {
            // connect
            if (!cameraDropdown.getSelectionModel().isEmpty()) {

                videoCapture.open(cameraDropdown.getSelectionModel().getSelectedItem());
                cameraSettings = new CameraSettingsProvider(videoCapture);
                videoCapture.set(Videoio.CAP_PROP_FRAME_HEIGHT, cameraSettings.getMaxResolution().height);
                videoCapture.set(Videoio.CAP_PROP_FRAME_WIDTH, cameraSettings.getMaxResolution().width);
                LOGGER.log(Level.INFO, String.format("Resolution: %f x %f",
                        videoCapture.get(Videoio.CAP_PROP_FRAME_WIDTH),
                        videoCapture.get(Videoio.CAP_PROP_FRAME_HEIGHT)));

                webcamView.setPreserveRatio(true);
                webcamView.setFitHeight(cameraSettings.getMaxResolution().height);
                webcamView.setFitWidth(cameraSettings.getMaxResolution().width);

                Runnable frameGrabberRunnable = () -> {
                    Mat frame = grabFrame();
                    Image imageToShow = Utils.mat2Image(frame);
                    Utils.onFXThread(webcamView.imageProperty(), imageToShow);
                };

                // grab a frame every 33 ms (30 frames/sec)
                this.frameGrabber = Executors.newSingleThreadScheduledExecutor();
                this.frameGrabber.scheduleAtFixedRate(frameGrabberRunnable, 0, 33, TimeUnit.MILLISECONDS);
                setCameraConnectedUi(true);
            }
        }
    }

    @FXML
    public void refreshBleCheckboxChecked(ActionEvent actionEvent) {
        if (refreshBleCheckbox.isSelected()) {
            bleDiscoveryService = new BleDiscoveryService();
            bleDiscoveryService.setPeriod(Duration.seconds(3.0));
            bleDiscoveryService.setOnSucceeded(t ->
                    fillBleDeviceDropdown((ObservableList<BluetoothDeviceWrapper>) t.getSource().getValue()));
            bleDiscoveryService.start();
        } else {
            bleDiscoveryService.cancel();
        }
    }

    public void fillBleDeviceDropdown(ObservableList<BluetoothDeviceWrapper> deviceList) {
        bleDropdown.setItems(deviceList);
        if (bleDropdown.getSelectionModel().isEmpty()) {
            if (!deviceList.isEmpty()) {
                bleDropdown.getSelectionModel().selectFirst();
            }
        }
    }

    private void makeBleSerial(BluetoothDevice device, boolean doConnect) throws BleSerialException {
        bleSerial = new BleSerial(device,
                UART_SERVICE_UUID, UART_CHARACTERISTIC_UUID, UART_CHARACTERISTIC_LENGTH);
        if (doConnect) {
            bleSerial.connect();
        } else {
            bleSerial.initialize();
        }
        setBleSerialConnectedUi(true);
        bleSerial.enableConnectedNotifications(this::setBleSerialConnectedUi);
    }

    private Mat grabFrame() {
        try {
            videoCapture.read(lastFrame);
            if (logging) {
                logger.recordFrame(lastFrame);
            }
        } catch (Exception e) {
            System.err.println("Exception during frame fetching: " + e);
        }

        return lastFrame;
    }

    private void stopVideoCapture() {
        if (frameGrabber != null && !frameGrabber.isShutdown()) {
            try {
                frameGrabber.shutdown();
                frameGrabber.awaitTermination(33, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
            }
        }
        if (videoCapture.isOpened()) {
            videoCapture.release();
        }
    }

    public void setBleSerialConnectedUi(boolean bleSerialConnected) {
        this.bleSerialConnected = bleSerialConnected;
        bleStatusLabel.setText(String.format("BLE:%s connected", bleSerialConnected ? "" : " not"));
        connectBleButton.setText(bleSerialConnected ? "Disconnect" : "Connect");
        bleDropdown.setDisable(bleSerialConnected);
        refreshBleCheckbox.setSelected(false);
        bleDiscoveryService.cancel();
        refreshBleCheckbox.setDisable(bleSerialConnected);
    }

    public void setCameraConnectedUi(boolean cameraConnected) {
        this.cameraConnected = cameraConnected;
        cameraStatusLabel.setText(String.format("Camera:%s connected", cameraConnected ? "" : " not"));
        connectCameraButton.setText(cameraConnected ? "Disconnect" : "Connect");
        cameraDropdown.setDisable(cameraConnected);
        refreshCamerasButton.setDisable(cameraConnected);
    }

    public void setLoggingUi(boolean logging) {
        this.logging = logging;
        loggingStatusLabel.setText(logging ? "LIVE" : "Not logging");
        startLoggingButton.setText(logging ? "Stop logging" : "Start logging");
        connectCameraButton.setDisable(logging);
        connectBleButton.setDisable(logging);
    }

    public void close() {
        stopVideoCapture();
        if (logging) {
            logger.stopLogging();
        }
        if (Objects.nonNull(bleSerial)) {
            bleSerial.disconnect();
        }
        if (Objects.nonNull(bleDiscoveryService)) {
            bleDiscoveryService.cancel();
        }
    }
}
