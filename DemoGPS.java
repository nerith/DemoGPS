import java.io.InputStream;
import java.util.Scanner;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;

public final class DemoGPS {
    private Scanner io;
    private int numberOfPositions;
    private Position currentPosition;
    private ArrayList<Position> positions;

    /**
     * DemoGPS represents a device that reads in NMEA strings in
     * realtime from a GPS and uses them to create a rolling average
     * position which can be requested at any time.
     */
    public DemoGPS(InputStream io, int numberOfPositions) {
        this.io = new Scanner(io);
        this.numberOfPositions = numberOfPositions;
        this.currentPosition = null;
        this.positions = new ArrayList<>();
    }

    /**
     * Starts a new thread which will continuously read the NMEA strings
     * from the input. After reading a string, it will be processed by the
     * code for latitude, longitude, and other information.
     */
    public void start() {
        Thread thread = new Thread() {
            public void run() {
                StringBuilder buffer = new StringBuilder();
                
                while(true) {
                    synchronized(this) {
                        buffer.append(io.nextLine());
                    }

                    String[] contents = buffer.toString().split(",");

                    processNMEAString(buffer.toString());
                    buffer.setLength(0);
                }
            }
        };

        thread.start();
    }

    private void processNMEAString(String nmea) {
        String[] contents = nmea.split(",");
        double latitude = 0.0;
        double longitude = 0.0;

        switch(contents[0]) {
            case "$GPGGA":
                // The latitude position obtained from the NMEA string is in
                // the form ddmm.mmmmm. This should obtain the degrees and
                // the minutes and make it of the form dd.mmmmmmm.
                try {
                    latitude = Double.parseDouble(contents[2]) / 100.0;
                } catch(NumberFormatException e) {
                    return;
                }

                // Compensate for potential corruption by checking that the
                // parsed value is between 0 and 90
                if(latitude > 90 || latitude < 0) {
                    return;
                }

                System.out.println(latitude);

                if(contents[3].equals("S")) {
                    latitude *= -1;
                }

                try {
                    longitude = Double.parseDouble(contents[4]) / 1000.0;
                } catch(NumberFormatException e) {
                    return;
                }

                // Compensate for potential corruption by checking that the
                // parsed value is between 0 and 90
                if(longitude > 180 || longitude < 0) {
                    return;
                }

                System.out.println(longitude);

                if(contents[5].equals("W")) {
                    longitude *= -1;
                }

                break;
            case "$GPGSV":
                double azimuth = Double.parseDouble(contents[5]);
                double elevation = Double.parseDouble(contents[6]);
                break;
            default:
                // In case of corruption, where the data identifier is
                // unknown or if the NMEA string is not GSV or a GGA data,
                // we should ignore it and not add any new positions.
                return;
        }

        synchronized(this) {

            if(positions.size() == numberOfPositions) {
                positions.remove(0);
            }

            positions.add(new Position(latitude, longitude));

            if(getCurrentPosition() != null) {
                System.out.println("Position is " + getCurrentPosition().getLatitude() + " " +
                                   getCurrentPosition().getLongitude());
            }
        }
    }

    public synchronized Position getCurrentPosition() {

        if(positions.size() == 0) {
            return null;
        }
        else if(positions.size() == 1) {
            return positions.get(0);
        }

        double x = 0.0;
        double y = 0.0;
        double z = 0.0;

        for(int i = 0; i < positions.size(); i++) {
            Position position = positions.get(i);

            double latitude = position.getLatitude() * Math.PI / 180;
            double longitude = position.getLongitude() * Math.PI / 180;

            x += Math.cos(latitude) * Math.cos(longitude);
            y += Math.cos(latitude) * Math.sin(longitude);
            z += Math.sin(latitude);
        }

        currentPosition = calculateLatLong(x / positions.size(),
                                           y / positions.size(),
                                           z / positions.size());

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

        /**
         * Constructs a new Position with a specified latitude and longitude.
         */
        public Position(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        /**
         * @return This position's latitude coordinate
         */
        public double getLatitude() {
            return latitude;
        }

        /**
         * @return This position's longitude coordinate
         */
        public double getLongitude() {
            return longitude;
        }
    }

    /**
     * Calculates a position using x, y and z cartesian coordinates.
     *
     * This is necessary for calculating the current average position out of the
     * specified number of positions.
     *
     * @param x The average x cartesian coordinate
     * @param y The average y cartesian coordinate
     * @param z The average z cartesian coordinate
     *
     * @return A position represented by latitude and longitude
     */
    private Position calculateLatLong(double x, double y, double z) {
        return new Position(Math.atan2(z, Math.sqrt(x*x + y*y)) * 180 / Math.PI,
                            Math.atan2(y, x) * 180 / Math.PI);
    }

    public static void main(String[] args) {
        DemoGPS gps = new DemoGPS(System.in, 5);
        gps.start();
    }
}
