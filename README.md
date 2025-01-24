# JAVA-Swarm AI智能体框架

基于 openai/swarm 复刻的实验性智能体框架项目，内部接入 openai 和 DashScope JDK 调用。

用 Java 复刻的原因：Python 无法做到多个智能体多线程并发调用。因为现在 AI 基本都是调用云平台（比如 OpenAI 和阿里云的百炼平台），可以多并发调用，希望通过 JAVA 解决这个问题。

## 快速开始

示例代码

```java
import priv.dawn.swarm.api.AgentClient;
import priv.dawn.swarm.common.Agent;
import priv.dawn.swarm.common.AgentMessage;
import priv.dawn.swarm.domain.AgentClientFactory;
import priv.dawn.swarm.enums.ClientType;
import priv.dawn.swarm.enums.Roles;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

class Demo {
    public static void main(String[] args) {
        
        /*
         定义 Client
         ClientType 用于选择内部调用的接口，比如 DashScope。下面这些作用相同。
         AgentClient client = new OpenAIClient("sk-xx","base_url");
         AgentClient client = new DashScopeClient("sk-xx","base_url");
        */
        AgentClient client = AgentClientFactory.create("sk-xxx", "base_url", ClientType.OPEN_AI);

        // 定义 Agent
        Agent agent = Agent.builder()
                .name("小美")
                .instructions("你是一个生活工作助理。")
                .model("gpt-4o")
                .build();

        // 定义历史消息
        AgentMessage message = new AgentMessage();
        message.setRole(Roles.USER.value);
        message.setContent("介绍一下你自己");

        // 调用获得结果
        List<AgentMessage> runResult = client.run(agent, Collections.singletonList(message), 100);

    }
}
```

关于调用结果：如果中间涉及到了对工具的调用，会进行多轮对话，这些消息都将被返回。

### 工具调用

由于 java 没有 python 那样方便的解包手段，加上本来网络调用也只能返回 json，所以干脆直接改为 json 字符串作为入口参数的方式。

```java
import priv.dawn.swarm.common.Agent;
import priv.dawn.swarm.common.ToolFunction;
import priv.dawn.swarm.domain.AgentClientFactory;
import priv.dawn.swarm.domain.ToolRepository;
import priv.dawn.swarm.api.AgentClient;

class Demo {

    public static void main(String[] args) {

        // 实例一个工具仓库
        ToolRepository tools = new ToolRepository();
        // 注册函数
        tools.factory()
                .name("get_weather")
                .description("获取指定城市的当前天气描述。")
                .addParamDescription("city", "城市名")
                .functionCall(json -> "天气晴朗，温度适宜，没有严重空气污染。")
                .register();
        // 注册一个无入参函数
        tools.factory()
                .name("get_time")
                .description("获取当前时间。")
                .functionCall(json -> new Date().toString())
                .register();

        // 定义 Agent
        Agent agent = Agent.builder()
                .name("小美")
                .instructions("你是一个生活工作助理。")
                .model("gpt-4o")
                .functions(tools)
                .parallelToolCalls(true)
                // 只有 OpenAI 的 JDK 能并行调用
                .build();

        AgentClient client = AgentClientFactory.create("sk-xxx", "base_url", ClientType.OPEN_AI);
        AgentMessage msg = new AgentMessage();
        msg.setRole(Roles.USER.value);
        msg.setContent("现在几点了？成都天气怎么样？");

        // 调用获得结果
        List<AgentMessage> runResult = client.run(agent, Collections.singletonList(msg), 100);
    }
}
```

### 流式调用

```java
import io.reactivex.Flowable;
import priv.dawn.swarm.api.AgentClient;
import priv.dawn.swarm.common.Agent;
import priv.dawn.swarm.common.AgentMessage;
import priv.dawn.swarm.common.AgentStreamMessage;
import priv.dawn.swarm.domain.AgentClientFactory;
import priv.dawn.swarm.enums.ClientType;
import priv.dawn.swarm.enums.Roles;

import java.util.Collections;

class Demo {
    public static void main(String[] args) {
        AgentClient client = AgentClientFactory.create("sk-xxx", "base_url", ClientType.OPEN_AI);
        Agent agent = Agent.builder()
                .name("小美")
                .instructions("你是一个学术工作助理。")
                .model("gpt-4o")
                .build();
        AgentMessage message = new AgentMessage();
        message.setRole(Roles.USER.value);
        message.setContent("条理清晰地介绍 transformer 架构，不少于 500 字。");

        // 调用获得结果
        Flowable<AgentStreamMessage> streamResult = client.streamRun(agent, Collections.singletonList(message), 100);
        // Flowable 实现流式输出
        streamResult.blockingForEach(message -> System.out.print(message.getMessage().getContent()));

    }
}
```

## 未来需求

解决一下线程池和多线程并发的问题