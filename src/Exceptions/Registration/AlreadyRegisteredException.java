package Exceptions.Registration;

public class AlreadyRegisteredException extends RegistrationException {
    public AlreadyRegisteredException(String message) {
        super(message);
    }
}
