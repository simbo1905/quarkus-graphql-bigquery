package drw.model;

import java.time.LocalDate;


public class Group {

    public String id;
    public String name;
    public String createdBy;
    public LocalDate createdDate;
    public Boolean blacklisted;

    public Group(String id,String name, boolean blacklisted, String createdBy, LocalDate createdDate){
        this.id = id;
        this.name = name;
        this.blacklisted = blacklisted;
        this.createdBy = createdBy;
        this.createdDate = createdDate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDate getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDate createdDate) {
        this.createdDate = createdDate;
    }

    public Boolean isBlacklisted() {
        return blacklisted;
    }

    public void setBlacklisted(Boolean blacklisted) {
        this.blacklisted = blacklisted;
    }
}
