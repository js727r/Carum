package com.a101.carum.api.controller;

import com.a101.carum.api.dto.ReqPostUser;
import com.a101.carum.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping()
    public ResponseEntity createUser(@RequestBody ReqPostUser reqPostUser){
        userService.createUser(reqPostUser);
        return ResponseEntity.ok().build();
    }

}
