package examples.binarytree.reference;

import java.util.Random;

public class BinaryTree {
  protected class Node {
    // In an actual homework problem, these could be protected
    // But because of the package difference here it prevents the submission
    // from accessing them because they are in a different package.
    public Object value;
    public Node right;
    public Node left;

    Node(Object setValue) {
      value = setValue;
    }
  }

  protected Node root;

  public final void add(Object value, Random random) {
    add(root, value, random);
  }

  private void add(Node current, Object value, Random random) {
    if (current == null) {
      root = new Node(value);
    } else if (current.right == null) {
      current.right = new Node(value);
    } else if (current.left == null) {
      current.left = new Node(value);
    } else if (random.nextBoolean()) {
      add(current.right, value, random);
    } else {
      add(current.left, value, random);
    }
  }
}
