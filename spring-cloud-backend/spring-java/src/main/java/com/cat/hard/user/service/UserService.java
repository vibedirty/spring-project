package com.cat.hard.user.service;

import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cat.hard.user.entity.User;
import com.cat.hard.user.mapper.UserMapper;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

	@Resource
	private UserMapper userMapper;

	public Optional<User> findByUsername(String username) {
		LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<User>(User.class);
		queryWrapper.eq(User::getUsername, username);
		User user = userMapper.selectOne(queryWrapper);
		return Optional.ofNullable(user);
	}
}
