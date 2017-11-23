package tr.org.uni_talk.pojo;

import tr.org.uni_talk.util.CommonMethods;

public class User implements IPojo {
    private String userName = null;
    private String password = null;
    private  String userNumber = null;

    public User() {}

    public User(String userName) {
        this(userName, "");
    }

    public User(String user, String pass) {
        this.userName = CommonMethods.getBareJID(user);
        this.password = pass;
        this.userNumber = CommonMethods.getNumber(this.userName);
    }

    public void setUserName(String aUserName) { this.userName = aUserName;}
    public String getUserName() { return this.userName;}
    public boolean isUserNameSet() { return (this.userName != null && !this.userName.trim().isEmpty());}

    public void setPassword(String aPassword) { this.password = aPassword;}
    public String getPassword() { return this.password;}

    @Override
    public String toString() {
        return this.userName + " " + this.password;
    }

    public String getUserNumber() {
        return userNumber;
    }
}
