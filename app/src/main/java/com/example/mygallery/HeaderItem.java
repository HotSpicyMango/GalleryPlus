package com.example.mygallery;

public class HeaderItem {
    private final String dateText;
    private final int imageCount;

    public HeaderItem(String dateText, int imageCount) {
        this.dateText = dateText;
        this.imageCount = imageCount;
    }

    public String getDateText() {
        return dateText;
    }

    public int getImageCount() {
        return imageCount;
    }
}
