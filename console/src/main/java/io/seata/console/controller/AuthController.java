/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.console.controller;

import io.seata.console.config.WebSecurityConfig;
import io.seata.console.constant.Code;
import io.seata.console.security.User;
import io.seata.console.utils.JwtTokenUtils;
import io.seata.console.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

/**
 * auth user
 *
 * @author jameslcj wfnuser
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    @Autowired
    private JwtTokenUtils jwtTokenUtils;
    @Autowired
    private AuthenticationManager authenticationManager;

    /**
     * Whether the Seata is in broken states or not, and cannot recover except by being restarted
     *
     * @return HTTP code equal to 200 indicates that Seata is in right states. HTTP code equal to 500 indicates that
     * Seata is in broken states.
     */

    @PostMapping("/login")
    public Result login(HttpServletResponse response, @RequestBody User user) {
        // 通过用户名和密码创建一个 Authentication 认证对象，实现类为 UsernamePasswordAuthenticationToken
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword());

        try {
            //通过 AuthenticationManager（默认实现为ProviderManager）的authenticate方法验证 Authentication 对象
            Authentication authentication = authenticationManager.authenticate(authenticationToken);
            //将 Authentication 绑定到 SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);
            //生成Token
            String token = jwtTokenUtils.createToken(authentication);

            String authHeader = WebSecurityConfig.TOKEN_PREFIX + token;
            //将Token写入到Http头部
            response.addHeader(WebSecurityConfig.AUTHORIZATION_HEADER, authHeader);

            return Result.ofSuccess(authHeader);
        } catch (BadCredentialsException authentication) {
            return Result.ofError(Code.LOGIN_FAILED);
        }
    }
}
