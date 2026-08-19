package com.mp.service.impl;

import com.mp.common.Result;
import com.mp.domain.po.SysUser;
import com.mp.domain.vo.LoginVO;
import com.mp.mapper.SysUserMapper;
import com.mp.service.ISysUserService;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mp.utils.JwtUtil;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author author
 * @since 2026-08-17
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {

    @Override
    public Result<LoginVO> login(String username, String password) {
        //1. 根据用户名查询用户
        SysUser user = lambdaQuery().eq(SysUser::getUsername, username).one();
        // 2. 判断用户是否存在
        if (user == null) {
            return Result.error("用户名或密码错误");
        }
        // 3. 校验密码
        if(!user.getPassword().equals(password)){
            return Result.error("用户名或密码错误");
        }
        // 4. 生成 JWT
        String token = JwtUtil.generateToken(user.getId());

        LoginVO loginVO = new LoginVO(token);

        // 4. 登录成功
        return Result.success(loginVO);
    }
}
