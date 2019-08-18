package com.otniel;

public enum ClassIndex {
    ALEF("שיעור א"), BET("שיעור ב"), GIMEL("שיעור ג"), DALED("שיעור ד"), HEI("שיעור ה"),
    BOGRIM("שיעור ו ומעלה"), TZEVET("צוות הישיבה");
    private String str;
    ClassIndex(String s) {
        this.str = s;
    }

    @Override
    public String toString() {
        return str;
    }
}
