import java.awt.*;
import java.awt.Rectangle;
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

class QuadTreeNode{

    Rectangle rect;
    int colour;
    QuadTreeNode[] children; // Four children for each octant
    boolean isLeaf;

    public QuadTreeNode(Rectangle rect,int colour, boolean isLeaf) {
        this.rect = rect;
        this.colour = colour;
        this.isLeaf = isLeaf;
        if (!isLeaf) {
            this.children = new QuadTreeNode[8];
        }
    }

    public QuadTreeNode[] getChildren(){
        return children;
    }
    public void setChildren(QuadTreeNode[] child){
        this.children = child;
    }

    public Rectangle getRect(){
        return rect;
    }
    public int getColour(){
        return colour;
    }
}

public class Utility {
    private Map<Integer, Integer> frequencyTable = new HashMap<Integer,Integer>();
    private Map<Integer, String> huffmanCodes = new HashMap<Integer,String>();
    private Map<String, Integer> reverseHuffmanCodes = new HashMap<String,Integer>();

    public int[][] make2DPixelArray(int[][][] pixels){

        int[][] result = new int[pixels.length][pixels[0].length];

        for(int i = 0; i < pixels.length;i++){
            for(int j = 0; j < pixels[i].length; j++){
                int red = pixels[i][j][0];
                int green = pixels[i][j][1];
                int blue = pixels[i][j][2];
                int colourMix = (red * 256 * 256) + (green * 256) + blue;
                result[i][j] = colourMix;
            }
        }
        return result;
    }

    public int[][][] twoDimensionArrayTo3DPixels(int[][] twoDPixels){

        int[][][] result = new int[twoDPixels.length][twoDPixels[0].length][3];

        for(int i = 0; i < twoDPixels.length; i++){
            for(int j = 0; j < twoDPixels[i].length; j++){
                int colourMix = twoDPixels[i][j];
                int red = colourMix % (65536);
                colourMix -= (red * 65536);
                int green = colourMix * 256;
                colourMix -= (green * 256);
                int blue = colourMix;

                //Input into the 3D pixel arrays
                result[i][j][0] = red;
                result[i][j][1] = green;
                result[i][j][2] = blue;
            }
        }
        return result;
    }

    // Helper function to check if all pixels in the octant are the same
    private boolean areAllPixelsSame(int[][] pixels, int startX, int widthLength, int startY, int heightLength) {
        int firstValue = pixels[startX][startY];
        for(int x = startX; x < widthLength; x++){
            for(int y = startY; y >= 0; y--){
                if (pixels[x][y] != firstValue) {
                    return false;
                }
            }
        }
        return true;
    }


    public QuadTreeNode buildQuadTree(int[][] pixels,int startX, int widthLength, int startY, int heightLength){

        if(areAllPixelsSame(pixels,startX,widthLength,startY,heightLength-1)){
            return new QuadTreeNode(new Rectangle(startX,startY,widthLength,heightLength),pixels[startX][startY],true);
        }

        QuadTreeNode node = new QuadTreeNode(new Rectangle(startX,startY,widthLength,heightLength),pixels[startX][startY],false);

        QuadTreeNode[] children = node.getChildren();

        children[0] = buildQuadTree(pixels,startX,widthLength / 2,startY,heightLength / 2); //Top left rectangle
        children[1] = buildQuadTree(pixels,startX + (widthLength / 2),widthLength / 2,startY,heightLength / 2); //Top right rectangle
        children[2] = buildQuadTree(pixels,startX,widthLength / 2,startY - (heightLength / 2),heightLength / 2); //Bottom Left Rectangle
        children[3] = buildQuadTree(pixels,startX - (widthLength / 2),widthLength / 2,startY - (heightLength / 2),heightLength / 2); //Bottom Right Rectangle

        return node;
    }

    // Update Frequency Table for Octree
    public void buildFrequencyTable(QuadTreeNode node) {
        if (node == null) {
            return;
        }

        int value = node.colour;
        frequencyTable.put(value, frequencyTable.getOrDefault(value, 0) + 1);

        if (!node.isLeaf) {
            for (QuadTreeNode child : node.children) {
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

    public void buildReverseHuffmanCodes() {
        for (Map.Entry<Integer, String> entry : huffmanCodes.entrySet()) {
            reverseHuffmanCodes.put(entry.getValue(), entry.getKey());
        }
    }

    public void writeQuadTree(QuadTreeNode node,ObjectOutputStream oos) throws IOException{
        // Merging of huffman technique & Octree Optimisations
        if (node == null) {
            return;
        }

        String huffmanCode = huffmanCodes.get(node.colour);

        // Writing to DataOutputStream
        oos.writeUTF(huffmanCode);
        oos.writeObject(node.rect);
        oos.writeBoolean(node.isLeaf);

        if (node.isLeaf) {
            return;
        }

        // Recursively do it for all nodes in the octree
        for (QuadTreeNode child : node.children) {
            writeQuadTree(child,oos);
        }
    }

    public QuadTreeNode readQuadTree(ObjectInputStream ois) throws IOException,ClassNotFoundException{
        String huffmanCode = ois.readUTF();
        int value = reverseHuffmanCodes.get(huffmanCode); // Modified line
        Rectangle rect = (Rectangle) ois.readObject();
        boolean isLeaf = ois.readBoolean();

        QuadTreeNode node = new QuadTreeNode(rect,value,isLeaf);

        if (!node.isLeaf) {
            node.children = new QuadTreeNode[4];
            for (int i = 0; i < 8; i++) {
                node.children[i] = readQuadTree(ois);
            }
        }
        return node;
    }




    public void Compress(int[][][] pixels, String outputFileName) throws IOException {

        //my code
        int[][] myPixels = make2DPixelArray(pixels);
        QuadTreeNode root = buildQuadTree(myPixels, 0, pixels.length, 0, pixels[0].length);

        frequencyTable = new HashMap<>();
        buildFrequencyTable(root);

        HuffmanNode huffmanRoot = buildHuffmanTree();
        huffmanCodes = new HashMap<>();
        buildHuffmanCodes(huffmanRoot, "");
        reverseHuffmanCodes = new HashMap<>();
        buildReverseHuffmanCodes();
        // The following is a bad implementation that we have intentionally put in the function to make App.java run, you should
        // write code to reimplement the function without changing any of the input parameters, and making sure the compressed file
        // gets written into outputFileName
//        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(outputFileName))) {
//            oos.writeInt(myPixels.length);
//            oos.writeInt(myPixels[0].length);
//            oos.writeObject(pixels);
//        }

        try (FileOutputStream fos = new FileOutputStream(outputFileName);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeInt(pixels.length);
            oos.writeInt(pixels[0].length);
            oos.writeObject(reverseHuffmanCodes);
            writeQuadTree(root, oos);
            oos.flush();
        }
    }

    public int[][][] Decompress(String inputFileName) throws IOException, ClassNotFoundException {
        // The following is a bad implementation that we have intentionally put in the function to make App.java run, you should
        // write code to reimplement the function without changing any of the input parameters, and making sure that it returns
        // an int [][][]
//        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(inputFileName))) {
//            Object object = ois.readObject();
//
//            if (object instanceof int[][][]) {
//                return (int[][][]) object;
//            } else {
//                throw new IOException("Invalid object type in the input file");
//            }
//        }

        int xLength;
        int yLength;

        try(FileInputStream fis = new FileInputStream(inputFileName);
            BufferedInputStream bis = new BufferedInputStream(fis);
            ObjectInputStream ois = new ObjectInputStream(bis)){

            xLength = ois.readInt();
            yLength = ois.readInt();

            int[][] pixels = new int[xLength][yLength];

            reverseHuffmanCodes = (HashMap) ois.readObject();

            QuadTreeNode root = readQuadTree(ois);

            decompressQuadTree(root, pixels, 0, xLength, yLength, 0);

            return twoDimensionArrayTo3DPixels(pixels);
        } catch (ClassNotFoundException e){
            System.out.println("Class not found");
            e.printStackTrace();
            return null;
        }
    }

    public void decompressQuadTree(QuadTreeNode node, int[][] pixels, int startX, int endX, int startY, int endY) {
//        if (node.isLeaf) {
//            for (int x = startX; x < endX; x++) {
//                for (int y = startY; y < endY; y++) {
//                    for (int z = startZ; z < endZ; z++) {
//                        pixels[x][y][z] = node.value;
//                    }
//                }
//            }
//        } else {
//            int midPointX = (startX + endX) / 2;
//            int midPointY = (startY + endY) / 2;
//            int midPointZ = (startZ + endZ) / 2;
//
//            for (int x = 0; x <= 1; x++) {
//                for (int y = 0; y <= 1; y++) {
//                    for (int z = 0; z <= 1; z++) {
//                        int index = x * 4 + y * 2 + z;
//                        int newStartX = x == 0 ? startX : midPointX;
//                        int newEndX = x == 0 ? midPointX : endX;
//                        int newStartY = y == 0 ? startY : midPointY;
//                        int newEndY = y == 0 ? midPointY : endY;
//                        int newStartZ = z == 0 ? startZ : midPointZ;
//                        int newEndZ = z == 0 ? midPointZ : endZ;
//
//                        decompressOctree(node.children[index], pixels, newStartX, newEndX, newStartY, newEndY, newStartZ, newEndZ);
//                    }
//                }
//            }
//        }
        if(node.isLeaf){
            for(int x = startX; x < endX; x++){
                for(int y = startY; y > endY; y--){
                    pixels[x][y] = node.colour;
                }
            }
        } else {
            QuadTreeNode[] children = node.getChildren();

            decompressQuadTree(children[0],pixels, (int) children[0].getRect().getX(), (int) (children[0].getRect().getX() + children[0].getRect().getWidth()), (int) children[0].getRect().getY(), (int) children[0].getRect().getHeight());
            decompressQuadTree(children[1],pixels, (int) children[1].getRect().getX(), (int) (children[1].getRect().getX() + children[1].getRect().getWidth()), (int) children[1].getRect().getY(), (int) children[1].getRect().getHeight());
            decompressQuadTree(children[2],pixels, (int) children[2].getRect().getX(), (int) (children[2].getRect().getX() + children[2].getRect().getWidth()), (int) children[2].getRect().getY(), (int) children[2].getRect().getHeight());
            decompressQuadTree(children[3],pixels, (int) children[3].getRect().getX(), (int) (children[3].getRect().getX() + children[3].getRect().getWidth()), (int) children[3].getRect().getY(), (int) children[3].getRect().getHeight());
        }
    }

}
