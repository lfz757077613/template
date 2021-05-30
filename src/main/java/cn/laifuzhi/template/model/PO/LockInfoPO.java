package cn.laifuzhi.template.model.PO;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class LockInfoPO {
    private Long id;
    private String lockKey;
    private Date expireTime;
    private Date createTs;
    private Date updateTs;
}
