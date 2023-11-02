import java.io.*;

class QuadNode implements Serializable {
    int x, y, size;
    int[] color; // [R, G, B]
    QuadNode nw, ne, sw, se;

    public QuadNode(int x, int y, int size, int[] color) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.color = color;
        this.nw = null;
        this.ne = null;
        this.sw = null;
        this.se = null;
    }
}

class QuadTree implements Serializable {

    QuadNode root;

    // You can tune this threshold
    public static final double VARIANCE_THRESHOLD = 100;

    public QuadTree(int[][][] image, int x, int y, int size) {
        this.root = build(image, x, y, size);
    }

    // Recursive function to build the QuadTree
    private QuadNode build(int[][][] image, int x, int y, int size) {
        if (x >= image.length || y >= image[0].length) {
            return null; // Boundary case
        }

        int[] avgColor = calculateAverageColor(image, x, y, size);
        double variance = calculateVariance(image, x, y, size, avgColor);

        QuadNode node = new QuadNode(x, y, size, avgColor);

        if (size > 1 && variance > VARIANCE_THRESHOLD) {
            int halfSize = size >> 1; // using bitwise operation for efficiency
            node.nw = build(image, x, y, halfSize);
            node.ne = build(image, x + halfSize, y, halfSize);
            node.sw = build(image, x, y + halfSize, halfSize);
            node.se = build(image, x + halfSize, y + halfSize, halfSize);
        }

        return node;
    }

    private double calculateVariance(int[][][] image, int x, int y, int size, int[] avgColor) {
        double varR = 0.0, varG = 0.0, varB = 0.0;
        int count = 0;

        for (int i = x; i < x + size && i < image.length; i++) {
            for (int j = y; j < y + size && j < image[0].length; j++) {
                varR += Math.pow(image[i][j][0] - avgColor[0], 2);
                varG += Math.pow(image[i][j][1] - avgColor[1], 2);
                varB += Math.pow(image[i][j][2] - avgColor[2], 2);
                count++;
            }
        }

        // Calculate average variance across the color channels
        double avgVariance = (varR + varG + varB) / (3 * count);

        return avgVariance;
    }

    private int[] calculateAverageColor(int[][][] image, int x, int y, int size) {
        double totalR = 0, totalG = 0, totalB = 0;
        int count = 0;

        for (int i = x; i < x + size && i < image.length; i++) {
            for (int j = y; j < y + size && j < image[0].length; j++) {
                totalR += image[i][j][0];
                totalG += image[i][j][1];
                totalB += image[i][j][2];
                count++;
            }
        }

        return new int[] { (int) Math.round(totalR / count), (int) Math.round(totalG / count),
                (int) Math.round(totalB / count) };
    }

}

public class Utility {

    private static final int COLOR_CHANNELS = 3; // Constant for color channels

    public void Compress(int[][][] pixels, String outputFileName) throws IOException {
        QuadTree quadTree = new QuadTree(pixels, 0, 0, pixels.length);

        // Using buffered output stream
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(new FileOutputStream(outputFileName)))) {
            oos.writeInt(pixels.length);
            oos.writeInt(pixels[0].length);
            oos.writeObject(quadTree);
        }
    }

    public int[][][] Decompress(String inputFileName) throws IOException, ClassNotFoundException {
        int sizeX, sizeY;
        QuadTree quadTree;

        // Using buffered input stream
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(inputFileName)))) {
            sizeY = ois.readInt();
            sizeX = ois.readInt();
            Object object = ois.readObject();
            if (object instanceof QuadTree) {
                quadTree = (QuadTree) object;
            } else {
                throw new IOException("Invalid object type in the input file");
            }
        }

        int[][][] pixels = new int[sizeY][sizeX][COLOR_CHANNELS];
        reconstructImage(quadTree.root, pixels);

        return pixels;
    }

    private void reconstructImage(QuadNode node, int[][][] pixels) {
        if (node == null) {
            return;
        }

        if (node.nw == null && node.ne == null && node.sw == null && node.se == null) {
            // This is a leaf node, fill the pixels
            for (int i = node.x; i < node.x + node.size; i++) {
                for (int j = node.y; j < node.y + node.size; j++) {
                    pixels[i][j][0] = node.color[0];
                    pixels[i][j][1] = node.color[1];
                    pixels[i][j][2] = node.color[2];
                }
            }
            return;
        }

        reconstructImage(node.nw, pixels);
        reconstructImage(node.ne, pixels);
        reconstructImage(node.sw, pixels);
        reconstructImage(node.se, pixels);
    }
}
