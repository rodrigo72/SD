package Exceptions.Registration;

public class InvalidPasswordException extends RegistrationException {
    public InvalidPasswordException(String message) {
        super(message);
    }
}
