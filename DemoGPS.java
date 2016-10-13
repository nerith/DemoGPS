import java.io.InputStream;
import java.io.EOFException;
import java.io.IOException;

public final class DemoGPS {
    private InputStream io;
    private int numberOfPositions;
    private Position currentPosition = null;

    /**
     * DemoGPS represents a device that reads in NMEA strings in
     * realtime from a GPS and uses them to create a rolling average
     * position which can be requested at any time.
     */
    public DemoGPS(InputStream io, int numberOfPositions) {
        this.io = io;
        this.numberOfPositions = numberOfPositions;
    }

    /**
     * Starts a new thread which will continuously read the NMEA strings
     * from the input.
     */
    public void start() {
        Thread thread = new Thread() {
            public void run() {
                while(true) {
                    // Read from input
                    // Process information
                }
            }
        };

        thread.start();
    }

    public synchronized Position getCurrentPosition() {
        // If no positions have been read, there is no valid
        // position, so return a null value.
        GGAReader.translate("$GPGGA,224904.054,5159.5578,N,001131.000,E,1,04");
        return currentPosition;
    }

    /**
     * Position represents a coordinate using latitude and longitude
     * and the standard convention of positive for North and East and
     * negative for South and West.
     */
    static class Position {
        private double latitude;
        private double longitude;

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }
    }

    // Would make this a separate file
    static class GGAReader {
        public static void translate(String NMEAString) {
            String[] information = NMEAString.split(",");

            double latitude = Double.parseDouble(information[2]);
            double longitude = Double.parseDouble(information[4]);

            System.out.println(latitude);
            System.out.println(longitude);
        }
    }

    public static void main(String[] args) {
        DemoGPS gps = new DemoGPS(null, 0);
        gps.getCurrentPosition();
        gps.start();
    }
}
