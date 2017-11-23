package tr.org.uni_talk.pojo;

import android.support.annotation.NonNull;

public class Contact implements IPojo, Comparable<Contact> {
    private long id;
    private long jid;
    private String name;
    private String number;
    private String status;
    private String isUtUser;
    private boolean selectable;
    private boolean isOwner;

    public Contact() {
        this("","","");
    }

    public Contact(String name, String number) {
        this(name, number, "");
    }

    public Contact(String name, String number, String status) {
        setName(name);
        setNumber(number);
        setStatus(status);
    }

    public long getJid() {return jid;}

    public void setJid(long jid) {this.jid = jid;}

    public String getIsUtUser() {return isUtUser;}

    public void setIsUtUser(String isUtUser) {this.isUtUser = isUtUser;}

    public String getName() {
        return name;
    }

    public void setName(String contactName) {
        this.name = contactName;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String contactNumber) {this.number = contactNumber;}

    public String getStatus() {return status;}

    public void setStatus(String status) {
        if (status != null && !status.trim().isEmpty())
            this.status = status;
        else
            this.status = "Hi! I'm using UniTalk.";
    }

    @Override
    public boolean equals(Object other) {
        boolean result = false;
        if (other instanceof Contact) {
            Contact that = (Contact) other;
            result = (this.getNumber().equals(that.getNumber()));
        } else if (other instanceof String) {
            result = other.equals(this.getNumber());
        }
        return result;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean isSelectable() {
        return selectable;
    }

    public void setSelectable(boolean selectable) {
        this.selectable = selectable;
    }

    public boolean isOwner() {
        return isOwner;
    }

    public void setOwner(boolean owner) {
        isOwner = owner;
    }
    
    @Override
    public int compareTo(@NonNull Contact another) {
        return getName().compareTo(another.getName());
    }
}