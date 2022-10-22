import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.apache.commons.math3.complex.Complex;

public class MandelbrotSet extends JPanel {

    private final int MAX_ITERATIONS = 500;
    private final int NUM_THREADS = 256;

    private final int width = 960;
    private final int height = 720;

    private int scale; // pixels per unit
    private Complex center;

    private boolean fullyDrawn;

    private BufferedImage image;

    private final Color[] colors = new Color[] {
            new Color(66, 30, 15),
            new Color(25, 7, 26),
            new Color(9, 1, 47),
            new Color(4, 4, 73),
            new Color(0, 7, 100),
            new Color(12, 44, 138),
            new Color(24, 82, 177),
            new Color(57, 125, 209),
            new Color(134, 181, 229),
            new Color(211, 236, 248),
            new Color(241, 233, 191),
            new Color(248, 201, 95),
            new Color(255, 170, 0),
            new Color(204, 128, 0),
            new Color(153, 87, 0),
            new Color(106, 52, 3) };

    public static void main(String[] args) {
        MandelbrotSet mandelbrot = new MandelbrotSet();

        JFrame frame = new JFrame("Mandelbrot Set");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(mandelbrot);
        frame.setSize(mandelbrot.getPreferredSize());
        frame.pack();
        frame.setVisible(true);
    }

    public MandelbrotSet() {
        this.setPreferredSize(new Dimension(width, height));
        this.setVisible(true);
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Point clickPoint = e.getPoint();

                // IMPORTANT -- update center before updating scale because pointToComplex
                // method uses the current scale
                center = pointToComplex(clickPoint);
                scale *= 2;

                new Thread(MandelbrotSet.this::generateMandelbrot).start();
                new Thread(new DrawMandelbrot()).start();
            }
        });

        scale = 275;
        center = new Complex(-0.3, 0);

        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        generateMandelbrot();
    }

    public void generateMandelbrot() {
        for (int i = 0; i < NUM_THREADS; i++) {
            final int threadNum = i;
            Thread calcThread = new Thread(() -> {
                for (int x = threadNum * width / NUM_THREADS; x < (threadNum + 1) * width / NUM_THREADS; x++) {
                    for (int y = 0; y < height; y++) {
                        double iterations = numIterations(pointToComplex(x, y));
                        if (iterations < MAX_ITERATIONS) {
                            Color color1 = colors[((int) Math.floor(iterations)) % colors.length];
                            Color color2 = colors[((int) Math.floor(iterations) + 1) % colors.length];
                            image.setRGB(x, y, linearInterpolateColor(color1, color2, iterations % 1).getRGB());
                        } else {
                            image.setRGB(x, y, Color.BLACK.getRGB());
                        }
                    }
                }
            });
            calcThread.start();
        }
    }

    public double linearInterpolate(double v0, double v1, double t) {
        return v0 + t * (v1 - v0);
    }

    public Color linearInterpolateColor(Color c1, Color c2, double t) {
        int newR = (int) linearInterpolate(c1.getRed(), c2.getRed(), t);
        int newG = (int) linearInterpolate(c1.getGreen(), c2.getGreen(), t);
        int newB = (int) linearInterpolate(c1.getBlue(), c2.getBlue(), t);
        return new Color(newR, newG, newB);
    }

    public double numIterations(Complex c) {
        Complex z = Complex.ZERO;
        double n = 0;
        while (z.abs() <= 10 && n < MAX_ITERATIONS) {
            z = z.multiply(z).add(c);
            n++;
        }
        if (n < MAX_ITERATIONS) {
            double log_zn = Math.log(z.abs());
            double nu = Math.log(log_zn / Math.log(2)) / Math.log(2);
            n += 1 - nu;
        }
        return n;
    }

    public Complex pointToComplex(double x, double y) {
        double re = (x - width / 2.0 + scale * center.getReal()) / scale;
        double im = (y - height / 2.0 + scale * center.getImaginary()) / scale;
        return new Complex(re, im);
    }

    public Complex pointToComplex(Point p) {
        return pointToComplex(p.getX(), p.getY());
    }

    @Override
    public void paintComponent(Graphics g) {
        g.drawImage(image, 0, 0, this);
    }

    private class DrawMandelbrot implements Runnable {
        @Override
        public void run() {
            while (true) {
                repaint();
                try {
                    Thread.sleep(15);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}