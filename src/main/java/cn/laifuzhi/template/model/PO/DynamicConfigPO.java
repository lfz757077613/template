package cn.laifuzhi.template.model.PO;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Setter
@Getter
public class DynamicConfigPO {
    private String configKey;
    private String configJson;
    private Long version;
    private Date createTs;
    private Date updateTs;
}
