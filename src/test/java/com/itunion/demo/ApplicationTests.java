package com.itunion.demo;

import com.itunion.demo.common.Result;
import com.itunion.demo.mapper.UserMapper;
import com.itunion.demo.service.UserService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ApplicationTests {

    @Autowired
    private UserService userService;
    @Autowired
    private UserMapper userMapper;

    @Before
    public void cleanData(){
        userMapper.deleteAll();
    }

    @Test
    public void contextLoads() throws InterruptedException {
        int size = 100;
        CountDownLatch countDownLatch = new CountDownLatch(size);
        List<Result> results = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Random random = new Random();
                    String openId = "openId:" + random.nextInt(10);
                    results.add(userService.loginByWxLock(openId));
                    countDownLatch.countDown();
                }
            }).start();
        }

        countDownLatch.await();

        // 统计信息
        int loginSuccessNum = 0;
        int loginFailedNum = 0;
        for (Result result : results) {
            if (result.getCode() == 0) {
                loginSuccessNum++;
            }else{
                loginFailedNum++;
            }
        }
        System.out.println("登录成功总数：" + loginSuccessNum);
        System.out.println("登录失败总数：" + loginFailedNum);
    }

}
