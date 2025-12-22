import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码加密工具测试
 * 用于生成数据库初始化所需的 BCrypt 密文
 */
public class PasswordTest {

    @Test
    public void generateBcryptPassword() {
        // 1. 定义你要加密的明文密码
        String rawPassword = "123456";

        // 2. 创建加密器实例 (无需 Spring 注入，直接 new 即可)
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // 3. 执行加密
        String encodedPassword = encoder.encode(rawPassword);

        // 4. 打印结果
        System.out.println("=========================================");
        System.out.println("原始密码: " + rawPassword);
        System.out.println("BCrypt密文: " + encodedPassword);
        System.out.println("=========================================");

        // 5. (可选) 验证逻辑：证明这个密文确实能匹配原密码
        boolean matches = encoder.matches(rawPassword, encodedPassword);
        System.out.println("验证匹配结果: " + matches);
    }
}