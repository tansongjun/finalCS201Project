public class OctreeNode {
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
