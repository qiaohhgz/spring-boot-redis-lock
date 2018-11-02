package com.itunion.demo.service;

import com.itunion.demo.common.Result;

public interface UserService {
    Result loginByWx(String openId);

    Result loginByWxLock(String openId);
}
