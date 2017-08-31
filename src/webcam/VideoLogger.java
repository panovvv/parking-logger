package webcam;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.opencv.core.Core.FONT_HERSHEY_PLAIN;

public class VideoLogger {

    private final static Logger LOGGER = Logger.getLogger(VideoLogger.class.getName());

    private static final String DATETIME_FORMAT = "yyyy-MM-dd_HH.mm";
    private static final Scalar TEXT_COLOR = new Scalar(0, 0, 0);
    private static final Scalar TEXT_BG_COLOR = new Scalar(255, 255, 255);
    private static final double FONT_SCALE = 1.5;
    private static final int BOX_WIDTH = 400;
    private static final int BOX_HEIGHT = 100;
    private static final int TEXT_THICKNESS = 2;

    private VideoCapture videoCapture;
    private CameraSettingsProvider cameraSettings;
    private VideoWriter videoWriter = new VideoWriter();

    private boolean gotBeginning;
    private StringBuilder lastEntryBuilder;
    private String lastEntry;
    private int batteryCharge;
    private Boolean pirState;
    private int magnetX;
    private int magnetY;
    private int magnetZ;

    public VideoLogger(VideoCapture videoCapture, CameraSettingsProvider cameraSettings) {
        this.videoCapture = videoCapture;
        this.cameraSettings = cameraSettings;
    }

    public void startLogging() {
        String videoFilePath = String.format("%s.avi",
                DateTimeFormatter.ofPattern(DATETIME_FORMAT).format(LocalDateTime.now()));
        LOGGER.log(Level.INFO, String.format("Output path: %s", videoFilePath));

        LOGGER.log(Level.INFO, String.format("Resolution: %s", cameraSettings.getMaxResolution()));
        LOGGER.log(Level.INFO, String.format("FPS: %s", cameraSettings.getFps()));

        int fourCC = VideoWriter.fourcc('X', '2', '6', '4');
//        int fourCC = VideoWriter.fourcc('m', 'j', 'p', 'g');

        videoWriter.open(videoFilePath, fourCC, cameraSettings.getFps(), cameraSettings.getMaxResolution(), true);
        gotBeginning = false;
        lastEntryBuilder = new StringBuilder();
    }

    public void stopLogging() {
        videoWriter.release();
    }

    public void pushNewEntry(String s) {
        if (gotBeginning) {
            if (s.contains("e")) {
                lastEntryBuilder.append(s.substring(0, s.indexOf('e') + 1));

                lastEntry = lastEntryBuilder.toString();
                batteryCharge = getIntBetweenChars('/');
                pirState = getBoolBetweenChars('#');
                magnetX = getIntBetweenChars('x');
                magnetY = getIntBetweenChars('y');
                magnetZ = getIntBetweenChars('z');

                gotBeginning = false;
                lastEntryBuilder.setLength(0);
                lastEntryBuilder.append(s.substring(s.indexOf('e') + 1));
            } else {
                lastEntryBuilder.append(s);
            }
        } else {
            if (s.contains("b")) {
                gotBeginning = true;
                lastEntryBuilder.append(s.substring(s.indexOf('b')));
            }
        }
    }

    private Integer getIntBetweenChars(char c) {
        return Integer.parseInt(getStringBetweenCharacters(c));
    }

    private Boolean getBoolBetweenChars(char c) {
        return "1".equals(getStringBetweenCharacters(c));
    }

    private String getStringBetweenCharacters(char c) {
        return lastEntry.substring(lastEntry.indexOf(c) + 1, lastEntry.lastIndexOf(c));
    }

    public void recordFrame(Mat frame) {
        if (Objects.nonNull(pirState)) {
            Imgproc.fillPoly(frame, Collections.singletonList(new MatOfPoint(new Point(0, 0),
                    new Point(BOX_WIDTH, 0),
                    new Point(BOX_WIDTH, BOX_HEIGHT),
                    new Point(0, BOX_HEIGHT))), TEXT_BG_COLOR);
            Imgproc.putText(frame,
                    String.format("Battery:%d%%", batteryCharge),
                    new Point(10, 20),
                    FONT_HERSHEY_PLAIN, FONT_SCALE, TEXT_COLOR, TEXT_THICKNESS);
            Imgproc.putText(frame,
                    String.format("PIR: %s", pirState ? "ON" : "OFF"),
                    new Point(10, 50),
                    FONT_HERSHEY_PLAIN, FONT_SCALE, TEXT_COLOR, TEXT_THICKNESS);
            Imgproc.putText(frame,
                    String.format("Magn: (%6s,%6s,%6s)", Integer.toString(magnetX),
                            Integer.toString(magnetY), Integer.toString(magnetZ)),
                    new Point(10, 80),
                    FONT_HERSHEY_PLAIN, FONT_SCALE, TEXT_COLOR, TEXT_THICKNESS);
        }
        videoWriter.write(frame);
    }
}
