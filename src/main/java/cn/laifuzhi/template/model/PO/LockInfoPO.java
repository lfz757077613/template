package cn.laifuzhi.template.model.PO;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class LockInfoPO {
    private long id;
    private String lockKey;
    private long expireTime;
    private Date createTs;
    private Date updateTs;
}
