
// Debug test to see what violations are being generated
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.isaac.dto.CreateUserDto;
import java.util.Set;

public class DebugTest {
    public static void main(String[] args) {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        // Test blank name
        CreateUserDto dto = new CreateUserDto("", "john.doe@example.com");
        Set<ConstraintViolation<CreateUserDto>> violations = validator.validate(dto);

        System.out.println("Number of violations: " + violations.size());
        for (ConstraintViolation<CreateUserDto> violation : violations) {
            System.out.println("Property: " + violation.getPropertyPath());
            System.out.println("Message: " + violation.getMessage());
            System.out.println("---");
        }
    }
}