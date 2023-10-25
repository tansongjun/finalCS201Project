import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class UtilityExperimentUsingOnlyHuffman {

    public void Compress(int[][][] pixels, String outputFileName) throws IOException {
        Map<Integer, Integer> frequencyTable = generateFreqTable(pixels);

        //Frequency Table to PriorityQueue
        PriorityQueue<HuffmanNode> nodes = generatePriorityQueue(frequencyTable);

        //Build Huffman Tree
        HuffmanNode huffmanTree = generateAHuffmanTree(nodes);

        // Generate Huffman codes
        Map<Integer, String> huffmanCodes = new HashMap<>();
        generateHuffmanCodes(huffmanTree, "", huffmanCodes);

        // Write the Huffman-encoded data
        writeEncodedData(pixels, outputFileName, huffmanCodes);
    }


    public int[][][] Decompress(String inputFileName) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(inputFileName))) {
            // Read Huffman codes
            Map<Integer, String> huffmanCodes = (Map<Integer, String>) ois.readObject();

            // Read dimensions of the array
            int depth = ois.readInt();
            int height = ois.readInt();
            int width = 3; // r, g, b

            int[][][] pixels = new int[depth][height][width];

            // Read the Huffman-encoded data and decode it
            for (int i = 0; i < depth; i++) {
                for (int j = 0; j < height; j++) {
                    for (int k = 0; k < width; k++) {
                        String code = ois.readUTF();
                        int value = decodeHuffmanCode(code, huffmanCodes);
                        pixels[i][j][k] = value;
                    }
                }
            }

            return pixels;
        }
    }


    //Function to take priority queue and convert into a huffman tree, returning the root node of the tree
    public HuffmanNode generateAHuffmanTree(PriorityQueue<HuffmanNode> priorityQueue) {
        // Build a Huffman tree
        while (priorityQueue.size() > 1) {
            HuffmanNode left = priorityQueue.remove();
            HuffmanNode right = priorityQueue.remove();
            HuffmanNode parent = new HuffmanNode(left, right);
            priorityQueue.add(parent);
        }
        return priorityQueue.peek();
    }


    private PriorityQueue<HuffmanNode> generatePriorityQueue(Map<Integer, Integer> frequencyTable) {

//        Set<Map.Entry<Integer, Integer>> entrySet = frequencyTable.entrySet();
//        PriorityQueue<HuffmanNode> priorityQueue = new PriorityQueue<>();
//        for (Map.Entry<Integer, Integer> entry : entrySet) {
//            HuffmanNode node = new HuffmanNode(entry.getKey(), entry.getValue());
//            priorityQueue.add(node);
//        }
//        return priorityQueue;

        return frequencyTable.entrySet().stream()
                .map(entry -> new HuffmanNode(entry.getKey(), entry.getValue()))
                .sorted()
                .collect(Collectors.toCollection(PriorityQueue::new));
//        List<HuffmanNode> nodes = new ArrayList<>();
//
//        for (Map.Entry<Integer, Integer> entry : frequencyTable.entrySet()) {
//            nodes.add(new HuffmanNode(entry.getKey(), entry.getValue()));
//        }
//        Collections.sort(nodes);
//
//        return nodes;
    }

    private Map<Integer, Integer> generateFreqTable(int[][][] pixels) {
        Map<Integer, Integer> frequencyTable = new HashMap<>();

        Arrays.stream(pixels)
                .forEach(plane -> Arrays.stream(plane)
                        .forEach(row -> Arrays.stream(row)
                                .forEach(value -> frequencyTable.put(value, frequencyTable.getOrDefault(value, 0) +
                                        1))
                        ));
//        for (int[][] plane : pixels) {
//            for (int[] row : plane) {
//                for (int value : row) {
//                    frequencyTable.put(value, frequencyTable.getOrDefault(value, 0) + 1);
//                }
//            }
//        }

        return frequencyTable;
    }

    private void generateHuffmanCodes(HuffmanNode node, String code, Map<Integer, String> huffmanCodes) {
        if (node.isLeaf()) {
            huffmanCodes.put(node.value, code);
        } else {
            generateHuffmanCodes(node.left, code + "0", huffmanCodes);
            generateHuffmanCodes(node.right, code + "1", huffmanCodes);
        }
    }

    private int decodeHuffmanCode(String code, Map<Integer, String> huffmanCodes) {
        for (Map.Entry<Integer, String> entry : huffmanCodes.entrySet()) {
            String huffmanCode = entry.getValue();
            if (code.startsWith(huffmanCode)) {
                // If the input code starts with a known Huffman code, return the corresponding value
                return entry.getKey();
            }
        }
        throw new IllegalArgumentException("Invalid Huffman code");
    }

    private void writeEncodedData(int[][][] pixels, String outputFileName, Map<Integer, String> huffmanCodes) throws IOException {

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(outputFileName))) {
            // Write Huffman codes and dimensions of to the output file);
            oos.writeObject(huffmanCodes);
            oos.writeInt(pixels.length);
            oos.writeInt(pixels[0].length);
            oos.writeInt(pixels[0][0].length);

            Arrays.stream(pixels)
                    .forEach(plane -> Arrays.stream(plane)
                            .forEach(row -> Arrays.stream(row)
                                    .forEach(value -> {
                                        String huffmanCode = huffmanCodes.get(value);
                                        BitSet huffmanCodeBit = new BitSet(huffmanCode.length());

                                        for (int i = 0; i < huffmanCode.length(); i++) {
                                            if (huffmanCode.charAt(i) == '1') {
                                                huffmanCodeBit.set(i);
                                            }
                                        }

                                        // Convert BitSet to byte array
                                        byte[] byteArray = huffmanCodeBit.toByteArray();

                                        // Write the byte array to the output stream
                                        try {
                                            oos.writeObject(byteArray);
                                        } catch (IOException e) {
                                            System.out.println(e.getMessage());
                                        }
                                    })
                            )
                    );
        }
    }
    public void traversePreOrder(StringBuilder sb, HuffmanNode node) {
        if (node != null) {
            sb.append(node.getValue());
            sb.append("\n");
            traversePreOrder(sb, node.getLeft());
            traversePreOrder(sb, node.getRight());
        }
    }

    private static class HuffmanNode implements Comparable<HuffmanNode> {
        int value;
        int frequency;
        HuffmanNode left;
        HuffmanNode right;

        public HuffmanNode(int value, int frequency) {
            this.value = value;
            this.frequency = frequency;
        }

        public HuffmanNode(HuffmanNode left, HuffmanNode right) {
            this.left = left;
            this.right = right;
            this.frequency = left.frequency + right.frequency;
        }

        public boolean isLeaf() {
            return left == null && right == null;
        }

        @Override
        public int compareTo(HuffmanNode other) {
            return Integer.compare(frequency, other.frequency);
        }

        public int getValue(){
            return value;
        }

        public HuffmanNode getLeft(){
            return left;
        }

        public HuffmanNode getRight(){
            return right;
        }
    }
}