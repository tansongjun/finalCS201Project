import java.io.*;
import java.util.*;

class HuffmanNode implements Comparable<HuffmanNode> {
    int value;
    int frequency;
    HuffmanNode left;
    HuffmanNode right;

    public HuffmanNode(int value, int frequency) {
        this.value = value;
        this.frequency = frequency;
    }

    @Override
    public int compareTo(HuffmanNode o) {
        return Integer.compare(this.frequency, o.frequency);
    }
}

class Node {
    int value;
    int frequency;
    Node left, right;

    public Node(int value, int frequency) {
        this.value = value;
        this.frequency = frequency;
    }
}

class QuadTree {
    int value;
    int height;
    int width;
    QuadTree[] children;

    boolean isLeaf;

    public QuadTree(int value, int height, int width) {
        this.value = value;
        this.height = height;
        this.width = width;
        this.children = null;
        this.isLeaf = true;
    }

    public QuadTree(int value, int height, int width, QuadTree[] children) {
        this.value = value;
        this.height = height;
        this.width = width;
        this.children = children;
        this.isLeaf = false;
    }
}

public class Utility {

    private final Map<Integer, Integer> frequencyTable = new HashMap<>();
    private final Map<Integer, String> huffmanCodes = new HashMap<>();

    private int index = 0;

    public void Compress(int[][][] pixels, String outputFileName) throws IOException {
        int[][] greyscale = convertToGreyscale(pixels);
        int[][] adjustedGreyscale = adjustImageDimensions(greyscale);
        System.out.println(adjustedGreyscale.length);
        System.out.println(adjustedGreyscale[0].length);
        int height = adjustedGreyscale.length;
        int width = adjustedGreyscale[0].length;

        // Build quadTree using greyscale
        QuadTree quadTree = buildQuadTree(adjustedGreyscale, 0, 0, height, width);

        // Huffman
        buildFrequencyTable(quadTree);
        HuffmanNode huffmanNode = buildHuffmanTree();
        buildHuffmanCodes(huffmanNode, "");


        String encodedData = encodeQuadTree(quadTree, huffmanCodes);

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(outputFileName))) {
            oos.writeInt(height);
            oos.writeInt(width);
            oos.writeObject(huffmanCodes);
            oos.writeObject(encodedData);
//            oos.writeUTF(encodedData);
        }
    }

    public int[][][] Decompress(String inputFileName) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(inputFileName))) {
            int height = ois.readInt();
            int width = ois.readInt();
            Map<Integer, String> huffmanCodes = (Map<Integer, String>) ois.readObject();
            String encodedData = (String) ois.readObject();

            QuadTree quadTree = decodeQuadTree(encodedData, huffmanCodes, height, width);
            int[][] greyscale = rebuildImage(quadTree, 0, 0, height, width);
            return convertToRGB(greyscale);
        }
    }

    public int[][] adjustImageDimensions(int[][] image) {
        int originalHeight = image.length;
        int originalWidth = image[0].length;

        int newHeight = Integer.highestOneBit(originalHeight - 1) << 1;
        int newWidth = Integer.highestOneBit(originalWidth - 1) << 1;

        int[][] newImage = new int[newHeight][newWidth];

        // Copy original image data
        for (int i = 0; i < originalHeight; i++) {
            System.arraycopy(image[i], 0, newImage[i], 0, originalWidth);
        }

        // Optionally, you can fill the rest of the newImage with some default value

        return newImage;
    }


    public void buildFrequencyTable(QuadTree node) {
        if (node.isLeaf) {
            int value = node.value;
            frequencyTable.put(value, frequencyTable.getOrDefault(value, 0) + 1);
        } else {
            for (QuadTree children : node.children) {
                buildFrequencyTable(children);
            }
        }
    }

    public HuffmanNode buildHuffmanTree() {

        // Create min heap to store frequency pairs
        PriorityQueue<HuffmanNode> queue = new PriorityQueue<>();

        // Turn all pairs into huffman nodes and add into heap
        for (Map.Entry<Integer, Integer> entry : frequencyTable.entrySet()) {
            queue.add(new HuffmanNode(entry.getKey(), entry.getValue()));
        }

        // Build huffman tree
        while (queue.size() > 1) {
            HuffmanNode left = queue.poll();
            HuffmanNode right = queue.poll();
            HuffmanNode parent = new HuffmanNode(-1, left.frequency + right.frequency);
            parent.left = left;
            parent.right = right;
            queue.add(parent);
        }

        return queue.poll();
    }

    public void buildHuffmanCodes(HuffmanNode node, String code) {
        // Base Case
        if (node == null) {
            return;
        }


        // Once leaf node is reached, place into hashmap to store codes
        if (node.left == null && node.right == null) {
            huffmanCodes.put(node.value, code);
        }

        // Get a unique binary code for each leaf node
        buildHuffmanCodes(node.left, code + "0");
        buildHuffmanCodes(node.right, code + "1");
    }



    public int[][] convertToGreyscale(int[][][] pixels) {
        int height = pixels.length;
        int width = pixels[0].length;
        int[][] greyscale = new int[height][width];

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int[] rgb = pixels[i][j];
                int grey = (int) (0.299 * rgb[0] + 0.587 * rgb[1] + 0.114 * rgb[2]);
                greyscale[i][j] = grey;
            }
        }
        return greyscale;
    }

    public QuadTree buildQuadTree(int[][] image, int startX, int startY, int height, int width) {
        if (height <= 0 || width <= 0) {
            throw new IllegalArgumentException("Height and width must be positive");
        }

        if (height == 1 && width == 1) {
            return new QuadTree(image[startX][startY], 1, 1);
        }

        int sum = 0;
        for (int x = startX; x < startX + height; x++) {
            for (int y = startY; y < startY + width; y++) {
                sum += image[x][y];
            }
        }
        int average = sum / (width * height);

        if (height == 1 || width == 1) {
            return new QuadTree(average, height, width);
        } else {
            int newHeight = height / 2;
            int newWidth = width / 2;

            QuadTree topLeft = buildQuadTree(image, startX, startY, newHeight, newWidth);
            QuadTree topRight = buildQuadTree(image, startX, startY + newWidth, newHeight, width - newWidth);
            QuadTree bottomLeft = buildQuadTree(image, startX + newHeight, startY, height - newHeight, newWidth);
            QuadTree bottomRight = buildQuadTree(image, startX + newHeight, startY + newWidth, height - newHeight, width - newWidth);

            QuadTree[] children = {topLeft, topRight, bottomLeft, bottomRight};
            return new QuadTree(average, height, width, children);
        }
    }


    public String encodeQuadTree(QuadTree quadTree, Map<Integer, String> huffmanCodes) {
        // Implement quadtree encoding here
        // ...
        StringBuilder encodedString = new StringBuilder();

        // Recursively traverse the QuadTree and encode it
        encodeQuadTreeHelper(quadTree, huffmanCodes, encodedString);

        return encodedString.toString();
    }

    public void encodeQuadTreeHelper(QuadTree quadTree, Map<Integer, String> huffmanCodes, StringBuilder encodedString) {
        if (quadTree == null) {
            return;
        }

        if (quadTree.isLeaf) {
            // If it's a leaf node, encode it as "1" followed by the Huffman code for the color
            encodedString.append("1");
            int value = quadTree.value;
            String huffmanCode = huffmanCodes.get(value);
            if (huffmanCode != null) {
                encodedString.append(huffmanCode);
            } else {
                throw new IllegalStateException("Huffman code not found for color: " + value);
            }
        } else {
            // If it's an internal node, encode it as "0" and then encode its children
            encodedString.append("0");
            for (QuadTree children : quadTree.children) {
                encodeQuadTreeHelper(children, huffmanCodes, encodedString);
            }
        }
    }

    public QuadTree decodeQuadTree(String encodedData, Map<Integer, String> huffmanCodes, int height, int width) {
        // Implement quadtree decoding here
        // ...
        if ((height == 1 && width == 1) || index >= encodedData.length()) {
            // If the block is a single pixel or we have reached the end of the encoded data,
            // decode the next value and return a leaf node
            String huffmanCode = getNextHuffmanCode(encodedData);
            Integer pixelVal = getPixelValueFromHuffmanCode(huffmanCode);
            if (pixelVal == null) {
                throw new IllegalArgumentException("Invalid huffman code in encoded data");
            }
            return new QuadTree(pixelVal, 1, 1);
        }

        char nodeType = encodedData.charAt(index++);
        if (nodeType == '1') {
            // If node is leaf node, decode value
            String huffmanCode = getNextHuffmanCode(encodedData);
            Integer pixelVal = getPixelValueFromHuffmanCode(huffmanCode);
            if (pixelVal == null) {
                throw new IllegalArgumentException("Invalid huffman code in encoded data");
            }
            return new QuadTree(pixelVal, height, width);
        } else if (nodeType == '0') {
            // If the node is a non-leaf node, recursively decode its children
            int newHeight = height / 2;
            int newWidth = width / 2;
            QuadTree topLeft = decodeQuadTree(encodedData, huffmanCodes, newHeight, newWidth);
            QuadTree topRight = decodeQuadTree(encodedData, huffmanCodes, newHeight, newWidth);
            QuadTree bottomLeft = decodeQuadTree(encodedData, huffmanCodes, newHeight, newWidth);
            QuadTree bottomRight = decodeQuadTree(encodedData, huffmanCodes, newHeight, newWidth);
            QuadTree[] children = new QuadTree[]{topLeft, topRight, bottomLeft, bottomRight};
            return new QuadTree(0, height, width, children);
        } else {
            throw new IllegalArgumentException("Invalid node type in encoded data");
        }
    }

    public String getNextHuffmanCode(String encodedData) {
        int start = index;
        while (index < encodedData.length() && Character.isDigit(encodedData.charAt(index))) {
            index++;
        }
        return encodedData.substring(start, index);
    }

    private Integer getPixelValueFromHuffmanCode(String huffmanCode) {
        for (Map.Entry<Integer, String> entry : huffmanCodes.entrySet()) {
            if (entry.getValue().equals(huffmanCode)) {
                return entry.getKey();
            }
        }
        return null; // Return null if the Huffman code is not found
    }

    private int[][] rebuildImage(QuadTree quadTree, int startX, int startY, int height, int width) {
        // Implement image rebuilding from quadtree here
        // ...
        int[][] image = new int[height][width];

        // Base case: if the quadtree node is a leaf node, fill the corresponding area of the image with the node's value
        if (quadTree.isLeaf) {
            for (int x = startX; x < startX + height; x++) {
                for (int y = startY; y < startY + width; y++) {
                    image[x][y] = quadTree.value;
                }
            }
        } else {
            // Recursive case: divide the area and process each child
            int newHeight = height / 2;
            int newWidth = width / 2;
            QuadTree[] children = quadTree.children;
            if (children != null && children.length == 4) {
                merge(image, rebuildImage(children[0], startX, startY, newHeight, newWidth));
                merge(image, rebuildImage(children[1], startX + newWidth, startY, newHeight, newWidth));
                merge(image, rebuildImage(children[2], startX, startY + newHeight, newHeight, newWidth));
                merge(image, rebuildImage(children[3], startX + newWidth, startY + newHeight, newHeight, newWidth));
            } else {
                throw new IllegalStateException("Node is not a leaf but has invalid number of children");
            }
        }

        return image;
    }

    private void merge(int[][] original, int[][] part) {
        for (int x = 0; x < part.length; x++) {
            for (int y = 0; y < part[x].length; y++) {
                original[x][y] = part[x][y];
            }
        }
    }

    public int[][][] convertToRGB(int[][] greyscale) {
        int height = greyscale.length;
        int width = greyscale[0].length;
        int[][][] rgb = new int[height][width][3];

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int grey = greyscale[i][j];
                rgb[i][j][0] = grey;
                rgb[i][j][1] = grey;
                rgb[i][j][2] = grey;
            }
        }
        return rgb;
    }
}
