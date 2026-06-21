package com.melina.notes.exception;

public class DemoCapacityExceededException extends RuntimeException {

    public DemoCapacityExceededException(int maxAccounts) {
        super("Maximum number of temporary demo accounts reached: " + maxAccounts);
    }
}
