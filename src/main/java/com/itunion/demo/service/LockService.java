package com.itunion.demo.service;

public interface LockService {

    void unLock(String key);

    boolean isLock(String key, int seconds);

    <T> T lockExecute(String key, LockExecute<T> lockExecute);

    interface LockExecute<T> {
        T execute();

        T waitTimeOut();
    }
}
