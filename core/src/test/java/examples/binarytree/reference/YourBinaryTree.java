package examples.binarytree.reference;

import edu.illinois.cs.cs125.answerable.*;
import edu.illinois.cs.cs125.answerable.api.Generators;

import java.util.Random;

public class YourBinaryTree extends BinaryTree {

    @Solution(name = "size")
    @Timeout(timeout = 1000)
    public int size() {
        return size(root);
    }
    private int size(Node current) {
        if (current == null) {
            return 0;
        } else {
            return 1 + size(current.left) + size(current.right);
        }
    }

    @Solution(name = "sum")
    @Timeout(timeout = 1000)
    public int sum() { return sum(root); }
    private int sum(Node current) {
        if (current == null) {
            return 0;
        } else {
            return (Integer) current.value + sum(current.left) + sum(current.right);
        }
    }

    @Generator
    public static YourBinaryTree ybtGen(int complexity, Random random) {
        YourBinaryTree ret = new YourBinaryTree();
        for (int i = 0; i < complexity; i++) {
            ret.add(Generators.defaultIntGenerator(complexity, random), random);
        }
        return ret;
    }

    @EdgeCase
    public static YourBinaryTree[] ybtEdgeCases() {
        return new YourBinaryTree[] { new YourBinaryTree(), null };
    }

    @SimpleCase
    public static YourBinaryTree[] ybtSimpleCases() {
        Random r = new Random(0); // mirroring should copy this so it will have the same seed

        YourBinaryTree one = new YourBinaryTree();
        one.add(1, r);

        YourBinaryTree three = new YourBinaryTree();
        three.add(1, r);
        three.add(2, r);
        three.add(3, r);

        return new YourBinaryTree[] { one, three };
    }

}
