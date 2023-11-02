import java.io.*;
import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;

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

    // Constant for color channels
    private static final int COLOR_CHANNELS = 3;

    // You can tune this threshold
    public static final double VARIANCE_THRESHOLD = 100;

    public QuadTree(int[][][] image, int x, int y, int size) {
        this.root = build(image, x, y, size);
    }

    private QuadNode build(int[][][] image, int x, int y, int size) {
        if (x >= image.length || y >= image[0].length) {
            return null; // Boundary case
        }

        int[] avgColor = new int[COLOR_CHANNELS];
        double variance = calculateVarianceAndAverageColor(image, x, y, size, avgColor);

        QuadNode node = new QuadNode(x, y, size, avgColor);

        if (size > 1 && variance > VARIANCE_THRESHOLD) {
            int halfSize = size >> 1;
            node.nw = build(image, x, y, halfSize);
            node.ne = build(image, x + halfSize, y, halfSize);
            node.sw = build(image, x, y + halfSize, halfSize);
            node.se = build(image, x + halfSize, y + halfSize, halfSize);
        }

        return node;
    }

    private double calculateVarianceAndAverageColor(int[][][] image, int x, int y, int size, int[] avgColor) {
        double totalR = 0, totalG = 0, totalB = 0;
        double varR = 0.0, varG = 0.0, varB = 0.0;
        int count = 0;

        for (int i = x; i < x + size && i < image.length; i++) {
            for (int j = y; j < y + size && j < image[0].length; j++) {
                int r = image[i][j][0];
                int g = image[i][j][1];
                int b = image[i][j][2];

                totalR += r;
                totalG += g;
                totalB += b;

                count++;
            }
        }

        avgColor[0] = (int) Math.round(totalR / count);
        avgColor[1] = (int) Math.round(totalG / count);
        avgColor[2] = (int) Math.round(totalB / count);

        for (int i = x; i < x + size && i < image.length; i++) {
            for (int j = y; j < y + size && j < image[0].length; j++) {
                varR += Math.pow(image[i][j][0] - avgColor[0], 2);
                varG += Math.pow(image[i][j][1] - avgColor[1], 2);
                varB += Math.pow(image[i][j][2] - avgColor[2], 2);
            }
        }

        return (varR + varG + varB) / (3 * count);
    }
}

public class Utility {

    private static final int COLOR_CHANNELS = 3; // Constant for color channels

    public int[][][] gaussianBlur(int[][][] image) {
        int[][][] result = new int[image.length][image[0].length][3];

        // Define a 3x3 Gaussian kernel (this is a simple one; in practice, you might
        // want a larger kernel)
        double[][] kernel = {
                { 1 / 16.0, 2 / 16.0, 1 / 16.0 },
                { 2 / 16.0, 4 / 16.0, 2 / 16.0 },
                { 1 / 16.0, 2 / 16.0, 1 / 16.0 }
        };

        for (int x = 1; x < image.length - 1; x++) {
            for (int y = 1; y < image[0].length - 1; y++) {
                for (int c = 0; c < 3; c++) { // for each color channel
                    double sum = 0.0;
                    for (int i = -1; i <= 1; i++) {
                        for (int j = -1; j <= 1; j++) {
                            sum += image[x + i][y + j][c] * kernel[i + 1][j + 1];
                        }
                    }
                    result[x][y][c] = (int) sum;
                }
            }
        }

        return result;
    }

    public void Compress(int[][][] pixels, String outputFileName) throws IOException {
        int[][][] processedPixels = gaussianBlur(pixels);
        QuadTree quadTree = new QuadTree(processedPixels, 0, 0, processedPixels.length);

        // Using GZIPOutputStream for post-compression
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(outputFileName))))) {
            oos.writeInt(pixels.length);
            oos.writeInt(pixels[0].length);
            oos.writeObject(quadTree);
        }
    }

    public int[][][] Decompress(String inputFileName) throws IOException, ClassNotFoundException {
        int sizeX, sizeY;
        QuadTree quadTree;

        // Using GZIPInputStream for decompression
        try (ObjectInputStream ois = new ObjectInputStream(
                new GZIPInputStream(new BufferedInputStream(new FileInputStream(inputFileName))))) {
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
