import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

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

    private HashMap<Integer, Integer> frequencyTable = new HashMap<>();
    private HashMap<Integer, String> huffmanCodes = new HashMap<>();
    private HashMap<String, Integer> reverseHuffmanCodes = new HashMap<>();

    public OctreeNode buildOctree(int[][][] pixels, int x0, int widthLength, int y0, int heightLength, int z0, int pixelLength) {
        // Check if all pixels in this octant are the same (or if max depth is reached)
        int firstValue = pixels[x0][y0][z0];
        boolean allSame = true;

        // Loop through each pixel, check if all pixels in the octant is the same
        for (int x = x0; x < widthLength; x++) {
            if (allSame == false) {
                break;
            }
            for (int y = y0; y < heightLength; y++) {
                if (allSame == false) {
                    break;
                }
                for (int z = z0; z < pixelLength; z++) {
                    if (allSame == false) {
                        break;
                    }
                    if (pixels[x][y][z] != firstValue) {
                        allSame = false;
                    }
                }
            }
        }

        // If pixels in octant are same, create leaf node
        if (allSame) {
            return new OctreeNode(firstValue, true);
        }

        // If pixels in octant are not all the same, split into 8 sub octants and recursively build them.
        OctreeNode node = new OctreeNode(0, false);

        // Get the midpoint of each lengths
        int midPointX = (x0 + widthLength) / 2;
        int midPointY = (y0 + heightLength) / 2;
        int midPointZ = (z0 + pixelLength) / 2;

        // Initialize index to 0
        int index = 0;

        for (int dx = 0; dx <= 1; dx++) {
            for (int dy = 0; dy <= 1; dy++) {
                for (int dz = 0; dz <= 1; dz++) {

                    node.children[index++] = buildOctree(pixels,
                            x0 + dx * (midPointX - x0), x0 + (dx + 1) * (midPointX - x0),
                            y0 + dy * (midPointY - y0), y0 + (dy + 1) * (midPointY - y0),
                            z0 + dz * (midPointZ - z0), z0 + (dz + 1) * (midPointZ - z0));
                }
            }
        }
        return node;
    }

    public void buildFrequencyTable(OctreeNode node) {

        // Base Case
        if (node == null) {
            return;
        }

        // Update node's value in frequency table;
        int value = node.value;// centrepoint for Division
        frequencyTable.put(value, frequencyTable.getOrDefault(value, 0) + 1);

        // Second Base Case, to prevent node.children from returning null
        if (node.isLeaf) {
            return;
        }

        // Recursively build frequency table for all children
        for (OctreeNode child : node.children) {
            buildFrequencyTable(child);
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

        String huffmanCode = huffmanCodes.get(node.value);

        //Writing to DataOutputStream
        dos.writeUTF(huffmanCode);
        dos.writeBoolean(node.isLeaf);

        if (node.isLeaf) {
            return;
        }

        //Recursively do it for all nodes in the octree
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

    public void decompressOctree(OctreeNode node, int[][][] pixels, int x0, int x1, int y0, int y1, int z0, int z1) {
        if (node.isLeaf) {
            for (int x = x0; x < x1; x++) {
                for (int y = y0; y < y1; y++) {
                    for (int z = z0; z < z1; z++) {
                        pixels[x][y][z] = node.value;
                    }
                }
            }
        } else {
            int mx = (x0 + x1) / 2, my = (y0 + y1) / 2, mz = (z0 + z1) / 2;

            int index = 0;
            for (int dx = 0; dx <= 1; dx++) {
                for (int dy = 0; dy <= 1; dy++) {
                    for (int dz = 0; dz <= 1; dz++) {
                        decompressOctree(node.children[index++], pixels,
                                x0 + dx * (mx - x0), x0 + (dx + 1) * (mx - x0),
                                y0 + dy * (my - y0), y0 + (dy + 1) * (my - y0),
                                z0 + dz * (mz - z0), z0 + (dz + 1) * (mz - z0));
                    }
                }
            }
        }
    }
}

//    Compress Execution Time for 10404007.png : 5 milliseconds
//        Size of the original file for 10404007.png: 502730 bytes
//        Size of the compressed file for 10404007.png: 582 bytes
//        Bytes saved from compression of 10404007.png: 502148 bytes
//        Decompress Execution Time for 10404007.png : 7 milliseconds
//        Mean Absolute Error of :10404007.png is 0.0
//        Mean Squared Error of :10404007.png is 0.0
//        PSNR of :10404007.png is Infinity