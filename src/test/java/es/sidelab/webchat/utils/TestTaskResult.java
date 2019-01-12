package es.sidelab.webchat.utils;

public class TestTaskResult {
    private long taskId;
    Boolean finished;

    public TestTaskResult(long taskId) {
        this.taskId = taskId;
        finished = false;
    }

    public void checkFinished() {
        finished = true;
    }

    public long getTaskId() {
        return this.taskId;
    }

    public Boolean isFinished() {
        return this.finished;
    }

    @Override
    public String toString() {
        return "{" + " taskId='" + getTaskId() + "'" + ", finished='" + isFinished() + "'"
                + "}";
    }

}