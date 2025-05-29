package win.ixuni.yonyoudatadict;


import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import win.ixuni.yonyoudatadict.service.DataDictService;

@SpringBootApplication
public class YonyouDataDictApplication {

    public static void main(String[] args) {
        SpringApplication.run(YonyouDataDictApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider yonYouDataDictTools(DataDictService dataDictService) {
        return MethodToolCallbackProvider.builder().toolObjects(dataDictService).build();
    }

    @Bean
    public CommandLineRunner startupRunner() {
        return args -> {
            System.out.println("=================================================================");
            System.out.println("                      ML数据字典已成功启动!                        ");
            System.out.println("=================================================================");
        };
    }

}