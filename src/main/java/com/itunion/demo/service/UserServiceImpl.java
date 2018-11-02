package com.itunion.demo.service;

import com.itunion.demo.common.Result;
import com.itunion.demo.domain.User;
import com.itunion.demo.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    private static Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private LockService lockService;

    @Override
    public Result loginByWx(String openId) {
        User user = userMapper.findByOpenId(openId);
        if (user != null) {
            return Result.of(0, "OK", user.getUserId());
        }

        // 模拟业务执行时间
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        log.info("create user by openId: {}", openId);
        user = new User();
        user.setOpenId(openId);
        userMapper.save(user);

        return Result.of(0, "OK", user.getUserId());
    }

    public Result loginByWxLock(String openId){
        // 更新点赞数量
        String lockKey = "lock:loginByWx:" + openId;

        return lockService.lockExecute(lockKey, new LockService.LockExecute<Result>() {
            @Override
            public Result execute() {
                return loginByWx(openId);
            }

            @Override
            public Result waitTimeOut() {
                return Result.of(-1, "访问太频繁");
            }
        });
    }
}
