package examples.binarytree.size;

import org.junit.jupiter.api.Assertions;
import java.util.Random;

import java.lang.reflect.Modifier;

public class ClassicBinaryTreeSizeTest {
    private static Random random = new Random();

    public static void testYourBinaryTreeSize() {
        int classModifiers = YourBinaryTree.class.getModifiers();

        Assertions.assertTrue(Modifier.isPublic(classModifiers), "Class should be public:");
        Assertions.assertFalse(Modifier.isFinal(classModifiers), "Class should not be final:");

        YourBinaryTree tree = new YourBinaryTree();
        Random random = new Random();
        Assertions.assertEquals(0, tree.size(), "Empty tree size incorrect:");
        tree.add(1, random);
        Assertions.assertEquals(1, tree.size(), "Single-item tree size incorrect:");
        tree.add(2, random);
        tree.add(3, random);
        Assertions.assertEquals(3, tree.size(), "Single-level tree size incorrect:");

        for (int count = 0; count < 256; count++) {
            int treeSize = random.nextInt(1024) + 4;
            tree = new YourBinaryTree();
            for (int i = 0; i < treeSize; i++) {
                tree.add(random.nextInt(1024), random);
            }
            Assertions.assertEquals(treeSize, tree.size(), "Tree size incorrect:");
        }
    }
}
