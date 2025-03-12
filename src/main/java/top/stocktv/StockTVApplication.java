package top.stocktv;

import lombok.extern.log4j.Log4j2;
import lombok.var;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

@Log4j2
@EnableWebSocket
@SpringBootApplication
public class StockTVApplication extends SpringBootServletInitializer {


    public static void main(String[] args) {
        ConfigurableApplicationContext application = SpringApplication.run(StockTVApplication.class, args);
        Environment env = application.getEnvironment();
        var port = env.getProperty("server.port");
        var banner = "\n----------------------------------------------------------\n\t" +
                "StockTV 启动成功！\n\t" +
                "请问访问该地址进行WS推送测试: \t\thttp://localhost:" + port + "/index.html\n" +
                "-------------------------------------------------------------------------";
        log.info(banner);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(StockTVApplication.class);
    }
}