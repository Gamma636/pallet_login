package com.pallet.pallet_login.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserRegisterRequest {
    private String username;
    private String mobile;
    private String password;
    private String base64Face;
}
