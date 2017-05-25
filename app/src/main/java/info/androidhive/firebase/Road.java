package info.androidhive.firebase;

/**
 * Created by user on 14.05.2017.
 */

public class Road {
    private String key;
    private int marks;
    private int countMarks;
    private String name;

    public Road(){

    }

    public Road(int marks, String name, int countMarks) {
        this.marks = marks;
        this.name = name;
        this.countMarks = countMarks;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public double getMark() {
        return marks;
    }

    public void setMark(int mark) {
        this.marks = mark;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCountMarks(){
        return countMarks;
    }

    public void setCountMarks(int countMarks){
        this.countMarks = countMarks;
    }

    public int getMarks() {
        return marks;
    }

    public void setMarks(int marks) {
        this.marks = marks;
    }
}
