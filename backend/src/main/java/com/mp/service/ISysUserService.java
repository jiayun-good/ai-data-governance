package com.mp.service;

import com.mp.common.Result;
import com.mp.domain.po.SysUser;
import com.baomidou.mybatisplus.spring.service.IService;
import com.mp.domain.vo.LoginVO;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author author
 * @since 2026-08-17
 */
public interface ISysUserService extends IService<SysUser> {

    Result<LoginVO> login(String username, String password);
}
