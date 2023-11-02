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

class OctreeNode {
    int value; // Average value of this node (or exact value for leaf nodes)
    OctreeNode[] children; // Eight children for each octant
    boolean isLeaf;

    public OctreeNode(int value, boolean isLeaf) {
        this.value = value;
        this.isLeaf = isLeaf;
        if (!isLeaf) {
            this.children = new OctreeNode[8];
        }
    }
}

public class Utility {
    // Class attributes
    private final Map<Integer, Integer> frequencyTable = new HashMap<>();
    private final Map<Integer, String> huffmanCodes = new HashMap<>();
    private final Map<String, Integer> reverseHuffmanCodes = new HashMap<>();

    // Helper function to check if all pixels in the octant are the same
    private boolean areAllPixelsSame(int[][][] pixels, int startX, int widthLength, int startY, int heightLength, int startZ,
            int pixelLength) {
        int firstValue = pixels[startX][startY][startZ];
        for (int x = startX; x < widthLength; x++) {
            for (int y = startY; y < heightLength; y++) {
                for (int z = startZ; z < pixelLength; z++) {
                    if (pixels[x][y][z] != firstValue) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public OctreeNode buildOctree(int[][][] pixels, int startX, int widthLength, int startY, int heightLength, int startZ,
            int pixelLength) {

        if (areAllPixelsSame(pixels, startX, widthLength, startY, heightLength, startZ, pixelLength)) {
            return new OctreeNode(pixels[startX][startY][startZ], true);
        }

        // Continue building the Octree for the non-leaf node
        // If pixels in octant are not all the same, split into 8 sub octants and
        // recursively build them.
        OctreeNode node = new OctreeNode(0, false);

        // Get the midpoint of each lengths
        int midPointX = (startX + widthLength) / 2;
        int midPointY = (startY + heightLength) / 2;
        int midPointZ = (startZ + pixelLength) / 2;

        // Initialize index to 0
        int index = 0;

        for (int axisDivideX = 0; axisDivideX <= 1; axisDivideX++) {
            for (int axisDivideY = 0; axisDivideY <= 1; axisDivideY++) {
                for (int axisDivideZ = 0; axisDivideZ <= 1; axisDivideZ++) {
                    node.children[index++] = buildOctree(pixels,
                            startX + axisDivideX * (midPointX - startX), startX + (axisDivideX + 1) * (midPointX - startX),
                            startY + axisDivideY * (midPointY - startY), startY + (axisDivideY + 1) * (midPointY - startY),
                            startZ + axisDivideZ * (midPointZ - startZ), startZ + (axisDivideZ + 1) * (midPointZ - startZ));
                }
            }
        }
        return node;
    }

    // Update Frequency Table for Octree
    public void buildFrequencyTable(OctreeNode node) {
        if (node == null) {
            return;
        }
        
        int value = node.value;
        frequencyTable.put(value, frequencyTable.getOrDefault(value, 0) + 1);

        if (!node.isLeaf) {
            for (OctreeNode child : node.children) {
                buildFrequencyTable(child);
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

    // Assign Huffman Codes to the Huffman Tree leaf nodes
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

    // New method to build reverse Huffman codes
    public void buildReverseHuffmanCodes() {
        for (Map.Entry<Integer, String> entry : huffmanCodes.entrySet()) {
            reverseHuffmanCodes.put(entry.getValue(), entry.getKey());
        }
    }

    public void writeOctree(OctreeNode node, DataOutputStream dos) throws IOException {
        // Merging of huffman technique & Octree Optimisations
        if (node == null) {
            return;
        }
        System.out.println("Compress node value: " + node.value);

        String huffmanCode = huffmanCodes.get(node.value);

        // Writing to DataOutputStream
        dos.writeUTF(huffmanCode);
        dos.writeBoolean(node.isLeaf);

        if (node.isLeaf) {
            return;
        }

        // Recursively do it for all nodes in the octree
        for (OctreeNode child : node.children) {
            writeOctree(child, dos);
        }

    }

    public OctreeNode readOctree(DataInputStream dis) throws IOException {
        String huffmanCode = dis.readUTF();
        int value = reverseHuffmanCodes.get(huffmanCode); // Modified line
        boolean isLeaf = dis.readBoolean();

        OctreeNode node = new OctreeNode(value, isLeaf);

        if (!node.isLeaf) {
            node.children = new OctreeNode[8];
            for (int i = 0; i < 8; i++) {
                node.children[i] = readOctree(dis);
            }
        }

        return node;
    }

    public void Compress(int[][][] pixels, String outputFileName) throws IOException {
        OctreeNode root = buildOctree(pixels, 0, pixels.length, 0, pixels[0].length, 0, pixels[0][0].length);

        buildFrequencyTable(root);

        // System.out.println("Testing for frequency table start");
        // for (Map.Entry<Integer, Integer> entry : frequencyTable.entrySet()) {
        // Integer key = entry.getKey();
        // Integer value = entry.getValue();
        // System.out.println("Key: " + key + ", Value: " + value);
        // }
        // System.out.println("Testing for frequency table end\n\n");

        HuffmanNode huffmanRoot = buildHuffmanTree();
        buildHuffmanCodes(huffmanRoot, "");

        try (DataOutputStream dos = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(outputFileName)))) {
            dos.writeInt(pixels.length);
            dos.writeInt(pixels[0].length);
            dos.writeInt(pixels[0][0].length);
            writeOctree(root, dos);
        }
    }

    public int[][][] Decompress(String inputFileName) throws IOException {
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(inputFileName)))) {
            int xLength = dis.readInt();
            int yLength = dis.readInt();
            int zLength = dis.readInt();

            buildReverseHuffmanCodes();

            int[][][] pixels = new int[xLength][yLength][zLength];
            OctreeNode root = readOctree(dis);

            decompressOctree(root, pixels, 0, xLength, 0, yLength, 0, zLength);

            return pixels;
        }
    }

    public void decompressOctree(OctreeNode node, int[][][] pixels, int startX, int endX, int startY, int endY, int startZ, int endZ) {

        System.out.println("Decompress Node Value" + node.value);
        if (node.isLeaf) {
            for (int x = startX; x < endX; x++) {
                for (int y = startY; y < endY; y++) {
                    for (int z = startZ; z < endZ; z++) {
                        pixels[x][y][z] = node.value;
//                        System.out.println();
                    }
                }
            }
        } else {
            int midPointX = (startX + endX) / 2;
            int midPointY = (startY + endY) / 2;
            int midPointZ = (startZ + endZ) / 2;

            for (int x = 0; x <= 1; x++) {
                for (int y = 0; y <= 1; y++) {
                    for (int z = 0; z <= 1; z++) {
                        int index = x * 4 + y * 2 + z;
                        int newStartX = x == 0 ? startX : midPointX;
                        int newEndX = x == 0 ? midPointX : endX;
                        int newStartY = y == 0 ? startY : midPointY;
                        int newEndY = y == 0 ? midPointY : endY;
                        int newStartZ = z == 0 ? startZ : midPointZ;
                        int newEndZ = z == 0 ? midPointZ : endZ;

                        decompressOctree(node.children[index], pixels, newStartX, newEndX, newStartY, newEndY, newStartZ, newEndZ);
                    }
                }
            }
        }
    }
}

// Compress Execution Time for 10404007.png : 5 milliseconds
// Size of the original file for 10404007.png: 502730 bytes
// Size of the compressed file for 10404007.png: 582 bytes
// Bytes saved from compression of 10404007.png: 502148 bytes
// Decompress Execution Time for 10404007.png : 7 milliseconds
// Mean Absolute Error of :10404007.png is 0.0
// Mean Squared Error of :10404007.png is 0.0
// PSNR of :10404007.png is Infinity