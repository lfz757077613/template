package cn.laifuzhi.template.model.PO;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class OrderInfoPO {
    private long id;
    private long uid;
    private long orderId;
    /**
     * @see cn.laifuzhi.template.model.enumeration.OrderStateEnum
     */
    private int state;
    private Date createTs;
    private Date updateTs;
}
