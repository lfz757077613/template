package cn.laifuzhi.template;

import org.junit.jupiter.api.Test;

class ApplicationTests {

    @Test
    void contextLoads() {
        Runtime.getRuntime().addShutdownHook(new Thread(System.out::println));
    }

}
