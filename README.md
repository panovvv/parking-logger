Parking logger code

![screenshot](https://github.com/panovvv/parking-logger/raw/master/media/screenshot.png?raw=true)

This can be used as an example of recording a video file from a camera with some overlayed data (sensors, etc.) using OpenCV for Java, or as an example of using TinyB library to connect to BLE devices in Java code.

Project is written using IntelliJ IDEA Community Edition.

This code needs Linux to run, because tinyb library makes use of Bluez API.
Linux dependencies:
  > git<br />
  > qt5-base<br />
  > cmake<br />
  > doxygen<br />
  > bluez<br />
  > bluez-utils<br />

## Preparations

- Clone this repository

  ```
  $ git clone https://github.com/panovvv/parking-logger.git
  ```
- Clone [OpenCV](https://github.com/opencv/opencv) and build it as described [here:](http://opencv-java-tutorials.readthedocs.io/en/latest/01-installing-opencv-for-java.html)

  ```
  $ git clone https://github.com/opencv/opencv.git
  $ cd opencv
  $ mkdir build
  $ cd build
  $ cmake-gui ..
  $ make
  $ sudo make install

  ```

- Clone [TinyB](https://github.com/intel-iot-devkit/tinyb) and build it as described [here.](https://shortn0tes.blogspot.com/2017/08/uart-bridge-from-hm-10-to-pc-with-java.html)

- Open this project in IntelliJ IDEA, adjusting library paths and native library paths if necessary (F4 - project properties)

- Run **Main** class. Connect your camera and BLE adapter. You're ready to start logging! 

Videolog sample (more in /media):
![Video log sample, GIF 30ish Mb](https://github.com/panovvv/parking-logger/raw/master/media/out.gif)


