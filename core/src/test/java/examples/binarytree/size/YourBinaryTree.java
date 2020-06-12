package examples.binarytree.size;

import examples.binarytree.reference.BinaryTree;

public class YourBinaryTree extends BinaryTree {

  public int size() {
    return size(root);
  }

  private int size(Node current) {
    if (current == null) {
      return 0;
    }

    return 1 + size(current.left) + size(current.right);
  }
}
