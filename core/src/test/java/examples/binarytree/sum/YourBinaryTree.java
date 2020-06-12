package examples.binarytree.sum;

import examples.binarytree.reference.BinaryTree;

public class YourBinaryTree extends BinaryTree {

  public int sum() {
    return sum(root);
  }

  private int sum(Node current) {
    if (current == null) {
      return 0;
    } else {
      return (Integer) current.value + sum(current.left) + sum(current.right);
    }
  }
}
