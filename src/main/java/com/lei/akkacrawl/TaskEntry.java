package com.lei.akkacrawl;

/**
 * Created by cl on 2016/10/16.
 */
public class TaskEntry implements Comparable<TaskEntry> {
    private Thread thread;
    private int retry;
    private TaskType type;
    private State state;

    public String getDownloadDest() {
        return downloadDest;
    }

    private String downloadDest;

    public CURL getCurl() {
        return curl;
    }

    private CURL curl;

    public TaskEntry(CURL curl, TaskType type, State state, int retry) {
        this.curl = curl;
        this.type = type;
        this.state = state;
        this.retry = retry;
    }

    public TaskEntry(CURL curl, TaskType type, State state, int retry, String downloadDest) {
        this(curl, type, state, retry);
        this.downloadDest = downloadDest;
    }

    public TaskEntry(CURL curl) {
        this.curl = curl;
        this.type = TaskType.GET;
        this.state = State.ENROLL;
        this.retry = 0;
    }

    @Override
    public int compareTo(TaskEntry o) {
        return this.curl.compareTo(o.curl);
    }

    public static enum TaskType {
        GET, DOWNLOAD, DONE;
    }

    public static enum State {
        ENROLL, RUNNING, DONE, ERROR;
    }

    public Thread getThread() {
        return thread;
    }

    public void setThread(Thread thread) {
        this.thread = thread;
    }

    public int getRetry() {
        return retry;
    }

    public void setRetry(int retry) {
        this.retry = retry;
    }

    public TaskType getType() {
        return type;
    }

    public void setType(TaskType type) {
        this.type = type;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    @Override
    public int hashCode() {
        return this.curl.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof TaskEntry) {
            TaskEntry temp = (TaskEntry) object;
            return this.curl.equals(temp.curl);
        }
        return false;
    }
}
