package model;

public class User {

    private int    id;
    private String username;
    private String role;        // STUDENT | LECTURER
    private String fullName;
    private String email;
    private String studentId;   // MSSV hoac ma GV
    private String deviceId;
    private String sessionToken;

    public User() {}

    // Getters
    public int    getId()           { return id; }
    public String getUsername()     { return username; }
    public String getRole()         { return role; }
    public String getFullName()     { return fullName; }
    public String getEmail()        { return email; }
    public String getStudentId()    { return studentId; }
    public String getDeviceId()     { return deviceId; }
    public String getSessionToken() { return sessionToken; }

    public boolean isLecturer()     { return "LECTURER".equals(role); }
    public boolean isStudent()      { return "STUDENT".equals(role); }

    // Setters
    public void setId(int id)                     { this.id = id; }
    public void setUsername(String username)       { this.username = username; }
    public void setRole(String role)               { this.role = role; }
    public void setFullName(String fullName)       { this.fullName = fullName; }
    public void setEmail(String email)             { this.email = email; }
    public void setStudentId(String studentId)     { this.studentId = studentId; }
    public void setDeviceId(String deviceId)       { this.deviceId = deviceId; }
    public void setSessionToken(String token)      { this.sessionToken = token; }

    @Override
    public String toString() {
        return "User{id=" + id + ", username=" + username + ", role=" + role + "}";
    }
}
