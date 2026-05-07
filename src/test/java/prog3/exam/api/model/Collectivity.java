package prog3.exam.api.model;

import java.util.List;

public class Collectivity extends CollectivityInformation {

    public String id;
    public String location;
    public CollectivityStructure structure;
    public List<Member> members;

    @Override
    public String toString() {
        return "Collectivity{" +
                "id='" + id + '\'' +
                ", location='" + location + '\'' +
                ", structure=" + structure +
                ", members=" + members +
                ", name='" + name + '\'' +
                ", number=" + number +
                '}';
    }
}