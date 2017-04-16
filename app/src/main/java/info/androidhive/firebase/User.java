package info.androidhive.firebase;

/**
 * Created by user on 17.03.2017.
 */

public class User {
    private String id;
    private String email;
    private String name;
    private int age;
    private String gender;

    public User(String email, String name, int age, String gender) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.age = age;
        this.gender = gender;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }
}
