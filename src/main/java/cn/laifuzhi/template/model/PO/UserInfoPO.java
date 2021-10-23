package cn.laifuzhi.template.model.PO;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class UserInfoPO {
    private long id;
    private String userName;
    private Date createTs;
    private Date updateTs;
}
