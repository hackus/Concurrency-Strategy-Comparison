package com.example.concurency.dbaccess;

import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class UserInfo {
    final Long id;
    final String name;

    public static UserInfo empty(){
        return new UserInfo(-1L, "");
    }

    public static String getAsString(Optional<UserInfo> result){
        return String.format("%s -> %s", result.orElseGet(UserInfo::empty).id, result.orElseGet(UserInfo::empty).name);
    }
}
