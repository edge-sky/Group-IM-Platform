package cn.lut.imserver.entity.vo;

import com.alibaba.fastjson2.JSONObject;

public class JoinRequestDto {

    private JSONObject inviteInfo;

    // Getter and Setter
    public JSONObject getInviteInfo() {
        return inviteInfo;
    }

    public void setInviteInfo(JSONObject inviteInfo) {
        this.inviteInfo = inviteInfo;
    }
}