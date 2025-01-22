import com.google.gson.Gson;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import priv.dawn.swarm.api.AgentClient;
import priv.dawn.swarm.common.Agent;
import priv.dawn.swarm.common.AgentMessage;
import priv.dawn.swarm.common.AgentStreamMessage;
import priv.dawn.swarm.common.ToolFunction;
import priv.dawn.swarm.domain.DashScopeClient;
import priv.dawn.swarm.domain.FunctionRepository;
import priv.dawn.swarm.enums.Roles;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Description:
 *
 * @author Dawn Yang
 * @since 2025/01/18/23:04
 */
@Slf4j
public class SimpleFunctionTest {

    @Test
    public void functionRepositoryTest() {
        FunctionRepository repository = new FunctionRepository();
        repository.factory()
                .name("Fuc1")
                .description("这是对 Fuc1 的描述！")
                .addParamDescription("name", "用户名称")
                .functionCall((str) -> {
                    System.out.println("Func! 在这" + str);
                    return "result";
                })
                .register();
        repository.factory()
                .name("F2")
                .description("这是 F2 的描述")
                .functionCall((str)->{
                    System.out.println("str = " + str);
                    return "F2 Result";
                })
                .register();
        ToolFunction fuc1 = repository.getTool("Fuc1");
        System.out.println("fuc1.getName() = " + fuc1.getName());
        System.out.println("fuc1.getDescription() = " + fuc1.getDescription());
        System.out.println("fuc1.getParameters() = " + fuc1.getParameters());
        ToolFunction f2 = repository.getTool("F2");
        String s = new Gson().toJson(f2);
        System.out.println("s = " + s);
    }

    @Test
    public void clientRunTest(){

        Agent agent = Agent.builder()
                .name("伊雷娜")
                .instructions("你是一个可爱的猫娘，是我们的生活助理。语气可爱一点，结尾需要加上”喵~“。")
                .model("qwen-turbo")
                .functions(getOne())
                .build();
        String apiKey = System.getenv("APIKEY");
//        AgentClient client = new OpenAIClient(apiKey,"https://dashscope.aliyuncs.com/compatible-mode/v1/");
        AgentClient client = new DashScopeClient(apiKey,"https://dashscope.aliyuncs.com/compatible-mode/v1/");
        AgentMessage msg = new AgentMessage();
        msg.setRole(Roles.USER.value);
        msg.setContent("现在几点了？成都天气怎么样？");
        List<AgentMessage> run = client.run(agent, Collections.singletonList(msg), 100);
        log.info("ANSWER: {}",new Gson().toJson(run));

    }

    @Test
    public void streamRunTest(){
        Agent agent = Agent.builder()
                .name("伊雷娜")
                .instructions("你是一个可爱的猫娘，是我们的生活助理。语气可爱一点，结尾需要加上”喵~“。")
                .model("qwen-turbo")
//                .functions(getOne())
                .build();
        String apiKey = System.getenv("APIKEY");
//        AgentClient client = new OpenAIClient(apiKey,"https://dashscope.aliyuncs.com/compatible-mode/v1/");
        AgentClient client = new DashScopeClient(apiKey,"https://dashscope.aliyuncs.com/compatible-mode/v1/");
        AgentMessage msg = new AgentMessage();
        msg.setRole(Roles.USER.value);
//        msg.setContent("现在几点了？成都天气怎么样？");
        msg.setContent("科普一下 Transformer 神经网络是什么？不少于500字");
        Flowable<AgentStreamMessage> streamRun = client.streamRun(agent, Collections.singletonList(msg), 100);
        Gson gson = new Gson();
        System.out.println(">>> ANSWER >>>");
        streamRun.blockingForEach(chunk-> System.out.println(chunk.getMessages().getContent()));
    }

    private FunctionRepository getOne(){
        FunctionRepository repository = new FunctionRepository();
        repository.factory()
                .name("get_weather")
                .description("获取指定城市的当前天气描述。")
                .addParamDescription("city","城市名")
                .functionCall(str -> {
                    log.debug("天气工具函数被调用: args={}",str);
                    return "天气晴朗，温度适宜，没有严重空气污染。";
                })
                .register();
        repository.factory()
                .name("get_time")
                .description("获取当前时间。")
                .functionCall(str->{
                    log.debug("时间工具函数被调用");
                    return new Date().toString();
                })
                .register();
        return repository;
    }

}
