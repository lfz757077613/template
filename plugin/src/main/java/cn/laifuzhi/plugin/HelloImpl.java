package cn.laifuzhi.plugin;

import cn.laifuzhi.api.Hello;
import cn.laifuzhi.api.Param;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.Extension;

@Slf4j
@Extension
public class HelloImpl implements Hello {
    private MyService service;

    public HelloImpl() {
        this.service = MyPlugin.getBean(MyService.class);
    }

    @Override
    public Param hello() {
        Param param = new Param();
        param.setSs("123");
        return param;
    }


}
