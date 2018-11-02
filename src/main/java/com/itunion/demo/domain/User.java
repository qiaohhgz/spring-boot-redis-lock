package com.itunion.demo.domain;

import javax.persistence.*;
import java.io.Serializable;

@Table(name = "sys_user")
@Entity
public class User implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userId;
    private String openId;

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getOpenId() {
        return openId;
    }

    public void setOpenId(String openId) {
        this.openId = openId;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", openId='" + openId + '\'' +
                '}';
    }
}
