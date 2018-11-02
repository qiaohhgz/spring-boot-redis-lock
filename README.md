# 架构实战篇（十八）：Spring Boot Redis实现分布式锁

## 前言
目前几乎很多大型网站及应用都是分布式部署的，分布式场景中的数据一致性问题一直是一个比较重要的话题。
在很多场景中，我们为了保证数据的最终一致性，需要很多的技术方案来支持，比如分布式事务、分布式锁等。

## Redis 的优点
* Redis有很高的性能
* Redis命令对此支持较好，实现起来比较方便

> 这也是选用Redis 做分布式锁的原因
> 启动 mysql 和 redis

## 一、先看下目录结构
![](https://upload-images.jianshu.io/upload_images/9260441-90b6e98c2bb53dc8.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

## 二、一般的业务代码

```java
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
```
如果在并发或者短时间内重复请求会出现重复创建用户的问题

## 三、了解Redis set方法

Jedis.set 方法
```java
public String set(final String key, final String value, final String nxxx, final String expx, final int time)

```
存储数据到缓存中，并制定过期时间和当Key存在时是否覆盖。
key 锁的名字
value 锁的内容
nxxx的值只能取NX或者XX，如果取NX，则只有当key不存在是才进行set，如果取XX，则只有当key已经存在时才进行set
expx的值只能取EX或者PX，代表数据过期时间的单位，EX代表秒，PX代表毫秒。
time 过期时间，单位是expx所代表的单位。


## 四、编写分布式锁

```java
package com.itunion.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

@Service
public class RedisLockServiceImpl implements LockService {

    private static Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final String LOCK_SUCCESS = "OK";

    @Autowired
    protected RedisTemplate<String, String> redisTemplate;

    @Override
    public void unLock(String key) {
        redisTemplate.delete(key);
    }

    public synchronized boolean isLock(String key, int seconds) {
        return redisTemplate.execute(new RedisCallback<Boolean>() {
            @Override
            public Boolean doInRedis(RedisConnection connection) throws DataAccessException {
                Jedis conn = (Jedis) connection.getNativeConnection();
                String result = conn.set(key, "1", "NX", "EX", seconds);
                return result != null && result.equalsIgnoreCase(LOCK_SUCCESS);
            }
        });
    }

    @Override
    public <T> T lockExecute(String key, LockExecute<T> lockExecute) {
        boolean isLock = isLock(key, 15);
        final int SLEEP_TIME = 200;
        final int RETRY_NUM = 20;
        int i;
        for (i = 0; i < RETRY_NUM; i++) {
            if (isLock) {
                break;
            }
            try {
                log.debug("wait redis lock key > {}", key);
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException e) {
                log.warn("wait redis error {}", e.getMessage());
            }
            isLock = isLock(key, 150);
        }
        if (!isLock) {
            log.warn("wait lock time out key > {}", key);
            return lockExecute.waitTimeOut();
        }
        try {
            if (i > 0) log.debug("wait lock retry count {}", i);
            return lockExecute.execute();
        } finally {
            unLock(key);
        }
    }
}
```


## 五、在业务上添加锁

```java
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
```

## 六、单元测试

```java
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
```

测试结果
![](https://upload-images.jianshu.io/upload_images/9260441-6ecaf40709cbe7e9.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

> Github:

#### 更多精彩内容
* [架构实战篇（一）：Spring Boot 整合MyBatis](https://www.jianshu.com/p/5f76bc4bb7cf)
* [架构实战篇（二）：Spring Boot 整合Swagger2](https://www.jianshu.com/p/57a4381a2b45)
* [架构实战篇（三）：Spring Boot 整合MyBatis(二)](https://www.jianshu.com/p/b0668bf8cf60)
* [架构实战篇（四）：Spring Boot 整合 Thymeleaf](https://www.jianshu.com/p/b5a854c0e829)
* [架构实战篇（五）：Spring Boot 表单验证和异常处理](https://www.jianshu.com/p/5152c065d3cb)
* [架构实战篇（六）：Spring Boot RestTemplate的使用](https://www.jianshu.com/p/c96049624891)
* [架构实战篇（七）：Spring Boot Data JPA 快速入门](https://www.jianshu.com/p/9beec5b84a38)
* [架构实战篇（八）：Spring Boot 集成 Druid 数据源监控](https://www.jianshu.com/p/da2b1a069a2b)
* [架构实战篇（九）：Spring Boot 分布式Session共享Redis](https://www.jianshu.com/p/44130d6754c3)
* [架构实战篇（十三）：Spring Boot Logback 邮件通知](https://www.jianshu.com/p/9b3a3f3a7e87)
* [架构实战篇（十四）：Spring Boot 多缓存实战](https://www.jianshu.com/p/e4aa6c86dd59)
* [架构实战篇（十五）：Spring Boot 解耦之事件驱动](https://www.jianshu.com/p/fcb9287483c8)
* [架构实战篇（十六）：Spring Boot Assembly服务化打包](https://www.jianshu.com/p/583e5a63953a)
* [架构实战篇（十七）：Spring Boot Assembly 整合 thymeleaf]
(https://www.jianshu.com/p/de773cc4316f)

#### 关注我们

![](https://upload-images.jianshu.io/upload_images/8122772-b78dee4c5818c874?imageMogr2/auto-orient/strip%7CimageView2/2/w/640/format/webp)