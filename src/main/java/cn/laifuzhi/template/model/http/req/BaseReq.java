package cn.laifuzhi.template.model.http.req;

import cn.laifuzhi.template.utils.CommonContext;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
public class BaseReq {
    public BaseReq() {
        this.uid = CommonContext.get().getUid();
    }

    @NotBlank
    private String regionId;
    private String uid;
}
