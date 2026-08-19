package com.mp.controller;


import com.mp.common.Result;
import com.mp.context.UserContext;
import com.mp.domain.dto.LoginDTO;
import com.mp.domain.vo.LoginVO;
import com.mp.service.ISysUserService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author author
 * @since 2026-08-17
 */
@RestController
@RequestMapping("/sys-user")
public class SysUserController {
    @Resource
    private ISysUserService sysUserService;

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO loginDTO){
        return sysUserService.login(loginDTO.getUsername(),loginDTO.getPassword());
    }

    @GetMapping("/info")
    public Result<?> info() {

        Long userId = UserContext.getUserId();

        System.out.println("当前登录用户：" + userId);

        return Result.success(userId);
    }
}
