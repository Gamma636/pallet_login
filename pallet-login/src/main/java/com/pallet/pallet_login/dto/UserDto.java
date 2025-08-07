package com.pallet.pallet_login.dto;

import com.pallet.pallet_login.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserDto {
    private Long userId;
    private String username;
    private String mobileNumber;
    public UserDto(UserEntity userEntity) {
        this.userId = userEntity.getId();
        this.username = userEntity.getUserName();
        this.mobileNumber = userEntity.getMobileNumber();
    }
}
