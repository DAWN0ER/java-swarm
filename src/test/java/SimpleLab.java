import com.google.gson.Gson;
import org.junit.Test;
import priv.dawn.swarm.common.ToolFunction;
import priv.dawn.swarm.domain.FunctionRepository;

/**
 * Created with IntelliJ IDEA.
 * Description:
 *
 * @author Dawn Yang
 * @since 2025/01/18/23:04
 */
public class SimpleLab {

    @Test
    public void t1() {
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

}
