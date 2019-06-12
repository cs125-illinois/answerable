package examples.binarytree.reference;

import edu.illinois.cs.cs125.answerable.EdgeCase;
import edu.illinois.cs.cs125.answerable.Generator;
import edu.illinois.cs.cs125.answerable.Helper;
import edu.illinois.cs.cs125.answerable.Solution;
import edu.illinois.cs.cs125.answerable.api.Generators;

import java.util.Random;

public class YourBinaryTree extends BinaryTree {

    @Solution(name = "size")
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

    @Generator
    public static YourBinaryTree ybtGen(int complexity, Random random) {
        YourBinaryTree ret = new YourBinaryTree();
        for (int i = 0; i < complexity; i++) {
            ret.add(Generators.defaultIntGenerator(complexity, random), random);
        }
        return ret;
    }

    @Helper
    @EdgeCase
    public static YourBinaryTree[] ybtEdgeCases() {
        return new YourBinaryTree[] { new YourBinaryTree() };
    }

}
