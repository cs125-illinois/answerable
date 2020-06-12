package examples.proxy;

public class FieldLinkedList {

  public FieldLinkedList next;
  public String value;

  public FieldLinkedList(String setValue) {
    value = setValue;
  }

  public void populateNext(String setNextValue) {
    next = new FieldLinkedList(setNextValue);
  }
}
