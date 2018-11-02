package com.itunion.demo.mapper;

import com.itunion.demo.domain.User;
import org.springframework.data.repository.CrudRepository;

public interface UserMapper extends CrudRepository<User, Integer> {
    User findByOpenId(String openId);
}
