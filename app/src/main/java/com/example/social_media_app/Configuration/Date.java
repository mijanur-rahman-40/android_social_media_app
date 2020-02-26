package com.example.social_media_app.Configuration;

public class Date {
    private Integer monthNameIndex;
    private String[] AllMonthName = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul",
            "Aug", "Sep", "Oct", "Nov",
            "Dec"};

    public Date(Integer monthNameIndex) {
        this.monthNameIndex = monthNameIndex - 1;
    }

    public String getMonthName() {
        return AllMonthName[monthNameIndex];
    }
}
