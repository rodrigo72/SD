package Server;

import Client.Registration;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import Exceptions.Registration.*;


public class RegistrationManager implements Serializable {
    private final ReadWriteLock l;
    private final Map<String, Registration> registrations;

    public RegistrationManager() {
        this.l = new ReentrantReadWriteLock();
        this.registrations = new HashMap<>();
    }

    public void register(String username, String password) throws AlreadyRegisteredException {
        Registration registration = new Registration(username, password, true);
        try {
            this.l.writeLock().lock();
            if (this.registrations.containsKey(registration.getName()))
                throw new AlreadyRegisteredException("Username " + username + " already exists");
            this.registrations.put(registration.getName(), registration);
        } finally {
            this.l.writeLock().unlock();
        }
    }

    public void removeRegistration(String username) throws RegistrationDoesNotExist {
        try {
            this.l.writeLock().lock();
            if (!this.registrations.containsKey(username))
                throw new RegistrationDoesNotExist("Username " + username + " does not exist");
            this.registrations.remove(username);
        } finally {
            this.l.writeLock().unlock();
        }
    }

    public void login(String username, String password) throws RegistrationDoesNotExist, InvalidPasswordException, AlreadyConnectedException {
        try {
            this.l.writeLock().lock();
            if (!this.registrations.containsKey(username))
                throw new RegistrationDoesNotExist("Username " + username + " does not exist");
            Registration registration = this.registrations.get(username);

            if (registration.getLoggedIn()) 
                throw new AlreadyConnectedException("Client is already connected on another device.");

            if (!registration.getPassword().equals(password))
                throw new InvalidPasswordException(password + " is not the correct password for " + username);

            registration.setLoggedInt(true);
        } finally {
            this.l.writeLock().unlock();
        }
    }

    public void logout(String username) throws RegistrationDoesNotExist {
        try {
            this.l.writeLock().lock();
            
            if (!this.registrations.containsKey(username))
                throw new RegistrationDoesNotExist("Username " + username + " does not exist");
            Registration registration = this.registrations.get(username);

            if (registration.getLoggedIn()) {
                registration.setLoggedInt(false);
            }
        } finally {
            this.l.writeLock().unlock();
        }
    }
}
