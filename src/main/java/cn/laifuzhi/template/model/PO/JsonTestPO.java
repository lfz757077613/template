package cn.laifuzhi.template.model.PO;

import com.alibaba.fastjson.JSON;
import com.alibaba.messaging.ops2.model.dto.ServerlessNLBZoneDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class JsonTestPO {
    private long id;
    private String lockKey;
    private String jsonStr;
    private transient ServerlessNLBZoneDTO jsonObj;
    private Date createTs;

    // 覆盖lombok的setting方法，更推荐这种方式，使用mybatis的typeHandler做json的序列化反序列化可以但没必要，本身在mysql的存储中，json类型就是一个特殊的varchar
    public void setJsonStr(String str) {
        this.jsonStr = str;
        this.jsonObj = JSON.parseObject(str, ServerlessNLBZoneDTO.class);
    }
}
