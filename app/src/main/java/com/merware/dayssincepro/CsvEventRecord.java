package com.merware.dayssincepro;

import java.util.Objects;

/**
 * Represents a single event record for CSV export and import.
 *
 * Encapsulates the event data model independently of database row structures,
 * facilitating clean data transfer between CSV files and SQLite storage.
 */
public class CsvEventRecord {

    private String category;
    private String event;
    private String date;
    private int recur;

    public CsvEventRecord() {
        this("", "", "", 0);
    }

    public CsvEventRecord(String event, String date, int recur) {
        this(null, event, date, recur);
    }

    public CsvEventRecord(String category, String event, String date, int recur) {
        this.category = category;
        this.event = event;
        this.date = date;
        this.recur = recur;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getRecur() {
        return recur;
    }

    public void setRecur(int recur) {
        this.recur = recur;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CsvEventRecord that = (CsvEventRecord) o;
        return recur == that.recur &&
                Objects.equals(category, that.category) &&
                Objects.equals(event, that.event) &&
                Objects.equals(date, that.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(category, event, date, recur);
    }

    @Override
    public String toString() {
        return "CsvEventRecord{" +
                "category='" + category + '\'' +
                ", event='" + event + '\'' +
                ", date='" + date + '\'' +
                ", recur=" + recur +
                '}';
    }
}
