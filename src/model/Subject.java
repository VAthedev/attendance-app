package model;

public class Subject {

    private int id;
    private String code; // MÃ MÔN
    private String name; // TÊN MÔN
    private int credits; // SỐ TÍN CHỈ
    private String description;
    private int semester; // HỌC KỲ
    private String department; // BỘ MÔN

    public Subject() {
    }

    public Subject(String code, String name, int credits, int semester) {
        this.code = code;
        this.name = name;
        this.credits = credits;
        this.semester = semester;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public int getCredits() {
        return credits;
    }

    public String getDescription() {
        return description;
    }

    public int getSemester() {
        return semester;
    }

    public String getDepartment() {
        return department;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCredits(int credits) {
        this.credits = credits;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setSemester(int semester) {
        this.semester = semester;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    @Override
    public String toString() {
        return "Subject{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", credits=" + credits +
                ", semester=" + semester +
                '}';
    }
}
