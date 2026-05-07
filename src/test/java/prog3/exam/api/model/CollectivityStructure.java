package prog3.exam.api.model;

public class CollectivityStructure {

    public Member president;
    public Member vicePresident;
    public Member treasurer;
    public Member secretary;

    @Override
    public String toString() {
        return "CollectivityStructure{" +
                "president=" + president +
                ", vicePresident=" + vicePresident +
                ", treasurer=" + treasurer +
                ", secretary=" + secretary +
                '}';
    }
}