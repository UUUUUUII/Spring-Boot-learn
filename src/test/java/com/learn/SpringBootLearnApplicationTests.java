package com.learn;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** 验证 Spring Boot 应用上下文及其配置、依赖注入可以正常启动。 */
@SpringBootTest(properties = "app.security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
class SpringBootLearnApplicationTests {

    /** 上下文成功加载即表示此启动冒烟测试通过。 */
    @Test
    void contextLoads() {
    }

}
