package es.sidelab.webchat.utils;

import java.util.Collection;

public class ConcurrentPrinter {
    private ConcurrentPrinter() {
    }

    private static ConcurrentPrinter instance = null;
	private static Object lock = new Object();

    public static ConcurrentPrinter getInstance() {
        synchronized (lock) {
            if (instance == null)
                instance = new ConcurrentPrinter();
        }
        return instance;
    }

    public void println(String s) {
        synchronized (this) {
            System.out.println(s);
        }
    }

    public void print(String s) {
        synchronized (this) {
            System.out.print(s);
        }
    }

    public void printWithTaskSignature(String s, long taskId) {
        synchronized (this) {
            System.out.println("======== TASK_ID" + taskId + " =============");
            System.out.println(s);
            System.out.println("======== END TASK_ID" + taskId + " =============");
        }
    }

    public void printEndWithTaskSignature(long taskId) {
        synchronized (this) {
            System.out.println("+++++++++++ FULL END TASK_ID" + taskId + " +++++++++++");
        }
    }

    public void printWithTaskSignature(Collection<?> data, String title) {
        printWithTaskSignature(data, title, -1);
    }

    public void printWithTaskSignature(Collection<?> data, String title, long taskId) {
        synchronized (this) {
            System.out.println("======== TASK_ID" + taskId + " =============");
            data.forEach((obj) -> System.out.println(title + " -> " + obj + " \n"));
            System.out.println("======== END TASK_ID" + taskId + " =============");
        }
    }

}
