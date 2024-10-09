package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

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

    @Override
    public Result sendCode(String phone, HttpSession session) {
        // check phone number
        // If don't match, return ERROR
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("Phone format wrong!");
        }
        // If does match, generate auth code
        String code = RandomUtil.randomNumbers(6);

        // 4. Save to session
        session.setAttribute("code", code);

        // Send code
        log.debug("Send auth code successfully, code is: " + code);
        // return OK
        return Result.ok();
    }
}
