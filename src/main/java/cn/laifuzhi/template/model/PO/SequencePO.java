package cn.laifuzhi.template.model.PO;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Setter
@Getter
public class SequencePO {
    private String sequenceKey;
    private long sequence;
    private Date createTs;
    private Date updateTs;
}
