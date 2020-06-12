package examples.classdesign.superclassmismatch.classes.reference;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class ExtAndImpl extends LinkedList implements List, Collection {}
