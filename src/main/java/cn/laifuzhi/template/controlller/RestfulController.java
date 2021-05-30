package cn.laifuzhi.template.controlller;

import cn.laifuzhi.template.aop.HttpBiz;
import cn.laifuzhi.template.model.http.resp.Resp;
import cn.laifuzhi.template.model.http.resp.RespEnum;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("rest")
public class RestfulController {

    @HttpBiz
    @GetMapping("get")
    private Resp<Void> get() {
        return Resp.build(RespEnum.OK);
    }
}
