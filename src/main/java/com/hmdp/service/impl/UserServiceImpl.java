package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpSession;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Qualifier("redisTemplate")
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        // check phone number
        // If don't match, return ERROR
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("Phone format wrong!");
        }
        // If does match, generate auth code
        String code = RandomUtil.randomNumbers(6);

        // 4. Save to Redis TTL 120 seconds
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);

        // Send code
        log.debug("Send auth code successfully, code is: " + code);
        // return OK
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // 1. Check against phone number
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("Phone format wrong!");
        }
        // 2. Check against auth code from Redis
        Object cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        String code = loginForm.getCode();
        System.out.println(cacheCode + " " + code);
        if (cacheCode == null || !cacheCode.toString().equals(code)) {
            // 3. Not the same, ERROR
            return Result.fail("Auth code is wrong!");
        }

        // 4. Consistent, search for user by phone number select * from tb_user where phone = ?
        User user = query().eq("phone", phone).one();
        // 5. Check if user exists
        // 6. Doesn't exist, create new user and save
        if (user == null) {
            user = createUserWithPhone(phone);
        }

        // 7. Save user info to Redis
        // 7.1. Generate random token
        String token = UUID.randomUUID().toString(true);
        // 7.2. User Object -> Hash
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO);
        // 7.3. Store
        stringRedisTemplate.opsForHash().putAll(LOGIN_CODE_KEY + token, userMap);
        stringRedisTemplate.expire(LOGIN_USER_KEY + token, LOGIN_USER_TTL, TimeUnit.MINUTES);
        // Nginx reverse proxy -> Tomcat, and session_id stored on client side cookie, so don't need to return anything
        // return token
        return Result.ok(token);
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX+ RandomUtil.randomString(10));
        // with MyBatis plus
        save(user);
        return null;
    }
}
