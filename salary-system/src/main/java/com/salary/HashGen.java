import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class HashGen {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        System.out.println("admin: " + encoder.encode("admin"));
        System.out.println("123456: " + encoder.encode("123456"));
    }
}
