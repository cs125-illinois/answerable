package examples.testgeneration.generators;

public class WildcardTypes {

    public <T extends Object> String callToString(T obj) {
        return obj.toString();
    }

}
