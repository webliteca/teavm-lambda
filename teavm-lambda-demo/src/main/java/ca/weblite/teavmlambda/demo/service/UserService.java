package ca.weblite.teavmlambda.demo.service;

import ca.weblite.teavmlambda.api.annotation.Inject;
import ca.weblite.teavmlambda.api.annotation.Service;
import ca.weblite.teavmlambda.api.annotation.Singleton;
import ca.weblite.teavmlambda.demo.dto.CreateUserRequest;
import ca.weblite.teavmlambda.demo.entity.User;
import ca.weblite.teavmlambda.demo.repository.UserRepository;

import java.util.List;

@Service
@Singleton
public class UserService {

    private final UserRepository userRepository;

    @Inject
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> listUsers() {
        return userRepository.findAll();
    }

    public User getUser(String id) {
        return userRepository.findById(id);
    }

    public User createUser(CreateUserRequest request) {
        return userRepository.create(request.getName(), request.getEmail());
    }

    public boolean deleteUser(String id) {
        return userRepository.deleteById(id);
    }
}
