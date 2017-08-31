package webcam;

import org.opencv.core.Size;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.util.*;
import java.util.logging.Logger;

public class CameraSettingsProvider {

    private final static Logger LOGGER = Logger.getLogger(CameraSettingsProvider.class.getName());

    private VideoCapture videoCapture;

    private final List<Size> allResolutions = Arrays.asList(
            new Size(640, 480),
            new Size(800, 600),
            new Size(1024, 576),
            new Size(1280, 720));

    private List<Size> supportedResolutions;
    private Size maxResolution;

    private Double fps;

    public CameraSettingsProvider(VideoCapture videoCapture) {
        this.videoCapture = videoCapture;
    }

    public List<Size> getSupportedResolutions() {
        if (Objects.isNull(supportedResolutions)) {
            searchForSupportedResolutions();
        }
        return supportedResolutions;
    }

    public Size getMaxResolution() {
        if (Objects.isNull(maxResolution)) {
            if (Objects.isNull(supportedResolutions)) {
                searchForSupportedResolutions();
            }
            maxResolution = supportedResolutions.stream()
                    .max(Comparator.comparingDouble(size -> size.width))
                    .orElse(new Size(videoCapture.get(Videoio.CAP_PROP_FRAME_WIDTH),
                            videoCapture.get(Videoio.CAP_PROP_FRAME_HEIGHT)));
        }
        return maxResolution;
    }

    private void searchForSupportedResolutions() {
        supportedResolutions = new ArrayList<>();
        for (Size resolution : allResolutions) {
            videoCapture.set(Videoio.CAP_PROP_FRAME_WIDTH, resolution.width);
            videoCapture.set(Videoio.CAP_PROP_FRAME_HEIGHT, resolution.height);
            if ((videoCapture.get(Videoio.CAP_PROP_FRAME_WIDTH) == resolution.width) &&
                    (videoCapture.get(Videoio.CAP_PROP_FRAME_HEIGHT) == resolution.height)) {
                supportedResolutions.add(resolution);
            }
        }
    }

    public double getFps() {
        if (Objects.isNull(fps)) {
            fps = videoCapture.get(Videoio.CAP_PROP_FPS);
        }
        return fps;
    }
}
