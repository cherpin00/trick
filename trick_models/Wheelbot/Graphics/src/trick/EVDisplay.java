/*
 * Trick
 * 2016 (c) National Aeronautics and Space Administration (NASA)
 */

package trick;

import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.net.Socket;
import java.util.*;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 *
 * @author penn
 */

class Feature {

    public double x, y;
    public double heading;
    public BufferedImage bufImage;

    public Feature(double X, double Y, double H, String imageFile) {
        x = X;
        y = Y;
        heading = H;

        Image img = new ImageIcon(imageFile).getImage();
        bufImage = new BufferedImage(img.getWidth(null), img.getHeight(null), 
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D gContext = bufImage.createGraphics();
        gContext.drawImage(img,0,0,null);
        gContext.dispose();
    }

    public void setState(double X, double Y, double H) {
        x = X;
        y = Y;
        heading = H;
    }
}

class ArenaMap extends JPanel {

    private List<Feature> featureList;
    private double unitsPerPixel;

    public ArenaMap(List<Feature> flist, double mapScale) {
        featureList = flist;
        unitsPerPixel = mapScale;
        SetScale(mapScale);
    }

    public void SetScale (double mapScale) {
        if (mapScale < 0.00001) {
            unitsPerPixel = 0.00001;
        } else {
            unitsPerPixel = mapScale;
        }
    }

    private void doDrawing(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        int width = getWidth();
        int height = getHeight();

        // Translate origin to the center of the panel.
        Graphics2D gCenter = (Graphics2D)g2d.create();
        gCenter.translate(width/2, height/2);

        for (int ii=0 ; ii < featureList.size() ; ++ii ) {
            Feature feature = featureList.get(ii);
            BufferedImage featureImage = feature.bufImage;
            int ImgOffsetX = featureImage.getWidth()/2;
            int ImgOffsetY = featureImage.getHeight()/2;
            int drawLocationX = (int)(feature.x / unitsPerPixel) - ImgOffsetX;
            int drawLocationY = (int)(feature.y / unitsPerPixel) - ImgOffsetY;
            AffineTransform tx = AffineTransform.getRotateInstance(feature.heading, ImgOffsetX, ImgOffsetY);
            AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
            gCenter.drawImage(op.filter(featureImage, null), drawLocationX, drawLocationY, null);
        }
        gCenter.dispose();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        doDrawing(g);
    }
}

public class EVDisplay extends JFrame {

    private ArenaMap arenaMap;
    private BufferedReader in;
    private DataOutputStream out;

    public EVDisplay(ArenaMap arena) {
        arenaMap = arena;
        add( arenaMap);
        setTitle("Vehicle Arena");
        setSize(800, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public void connectToServer(String host, int port ) throws IOException {
        Socket socket = new Socket(host, port);
        in = new BufferedReader( new InputStreamReader( socket.getInputStream()));
        out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
    }

    public void drawArenaMap() {
        arenaMap.repaint();
    }

    private static void  printHelpText() {
        System.out.println(
            "----------------------------------------------------------------------\n"
          + "usage: java jar EVDisplay.jar <port-number>\n"
          + "----------------------------------------------------------------------\n"
          );
    }

    public static void main(String[] args) throws IOException {

        String host = "localHost";
        int port = 0;
        String wayPointsFile = null;
        String vehicleImageFile = null;

        int ii = 0;
        while (ii < args.length) {
            switch (args[ii]) {
                case "-help" :
                case "--help" : {
                    printHelpText();
                    System.exit(0);
                } break;
                case "-v" : {
                    ++ii;
                    if (ii < args.length) {
                        vehicleImageFile = args[ii];
                    }
                } break;
                case "-w" : {
                    ++ii;
                    if (ii < args.length) {
                        wayPointsFile = args[ii];
                    }
                } break;
                default : {
                    port = (Integer.parseInt(args[ii]));
                } break;
            }
            ++ii;
        }

       if (port == 0) {
           System.out.println("No variable server port specified.");
           printHelpText();
           System.exit(0);
       }

       if (wayPointsFile == null) {
           System.out.println("No waypoints file specified. Use the -w option to specify a waypoints file.");
           printHelpText();
           System.exit(0);
       }

       if (vehicleImageFile == null) {
           System.out.println("No vehicle image file specified. Use the -v option to specify the vehicle image file.");
           printHelpText();
           System.exit(0);
       }

        List<Feature> featureList = new ArrayList<>();

        try ( BufferedReader br = new BufferedReader( new FileReader( wayPointsFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String field[] = line.split(",");
                double X = Double.parseDouble(field[0]);
                double Y = Double.parseDouble(field[1]);
                double H = Double.parseDouble(field[2]);
                String imageFileName = field[3];
                featureList.add(new Feature( X, Y, H, imageFileName));
            }
        }

        Feature vehicle = new Feature(0, 0, Math.toRadians(0), vehicleImageFile);
        featureList.add(vehicle);

        EVDisplay evd = new EVDisplay( new ArenaMap( featureList, 0.015));
        evd.setVisible(true);

        System.out.println("Connecting to: " + host + ":" + port);
        evd.connectToServer(host, port);

        evd.out.writeBytes("trick.var_set_client_tag(\"EVDisplay\") \n");
        evd.out.flush();

        evd.out.writeBytes("trick.var_add(\"veh.vehicle.position[0]\") \n" +
                           "trick.var_add(\"veh.vehicle.position[1]\") \n" +
                           "trick.var_add(\"veh.vehicle.heading\") \n");
        evd.out.flush();

        evd.out.writeBytes("trick.var_ascii() \n" +
                           "trick.var_cycle(0.1) \n" +
                           "trick.var_send() \n" );
        evd.out.flush();

        Boolean go = true;

        while (go) {
            String field[];
            try {
                String line;
                line = evd.in.readLine();
                field = line.split("\t");
                double X = Double.parseDouble(field[1]);
                double Y = Double.parseDouble(field[2]);
                double H = Double.parseDouble(field[3]) + 1.570796325;
                vehicle.setState(X,Y,H);

            } catch (IOException | NullPointerException e ) {
                go = false;
            }
            evd.drawArenaMap();
        }
    }
}
