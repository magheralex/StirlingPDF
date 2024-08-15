package stirling.software.SPDF.config.security;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import stirling.software.SPDF.model.Authority;
import stirling.software.SPDF.model.User;
import stirling.software.SPDF.repository.UserRepository;
@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Authentication getAuthentication(String apiKey) {
        User user = getUserByApiKey(apiKey);
        if (user == null) {
            throw new UsernameNotFoundException("API key is not valid");
        }
        
        // Convert the user into an Authentication object
        return new UsernamePasswordAuthenticationToken(
            user,                       // principal (typically the user)
            null,                       // credentials (we don't expose the password or API key here)
            getAuthorities(user)        // user's authorities (roles/permissions)
        );
    }
    
    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        // Convert each Authority object into a SimpleGrantedAuthority object.
    	return user.getAuthorities().stream()
    		    .map((Authority authority) -> new SimpleGrantedAuthority(authority.getAuthority()))
    		    .collect(Collectors.toList());


    }
    
    private String generateApiKey() {
        String apiKey;
        do {
            apiKey = UUID.randomUUID().toString();
        } while (userRepository.findByApiKey(apiKey) != null); // Ensure uniqueness
        return apiKey;
    }

    public User addApiKeyToUser(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        user.setApiKey(generateApiKey());
        return userRepository.save(user);
    }

    public User refreshApiKeyForUser(String username) {
        return addApiKeyToUser(username); // reuse the add API key method for refreshing
    }

    public String getApiKeyForUser(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return user.getApiKey();
    }

    public boolean isValidApiKey(String apiKey) {
        return userRepository.findByApiKey(apiKey) != null;
    }

    public User getUserByApiKey(String apiKey) {
        return userRepository.findByApiKey(apiKey);
    }
    
    public UserDetails loadUserByApiKey(String apiKey) {
        User userOptional = userRepository.findByApiKey(apiKey);
        if (userOptional != null) {
            User user = userOptional;
            // Convert your User entity to a UserDetails object with authorities
            return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(), // you might not need this for API key auth
                getAuthorities(user)
            );
        }
        return null;  // or throw an exception
    }
    

    public boolean validateApiKeyForUser(String username, String apiKey) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        return userOpt.isPresent() && userOpt.get().getApiKey().equals(apiKey);
    }
    
    public void saveUser(String username, String password) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEnabled(true);
        userRepository.save(user);
    }

    public void saveUser(String username, String password, String role, boolean firstLogin) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.addAuthority(new Authority(role, user));
        user.setEnabled(true);
        user.setFirstLogin(firstLogin);
        userRepository.save(user);
    }
    
    public void saveUser(String username, String password, String role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.addAuthority(new Authority(role, user));
        user.setEnabled(true);
        user.setFirstLogin(false);
        userRepository.save(user);
    }
    
    public void deleteUser(String username) {
    	 Optional<User> userOpt = userRepository.findByUsername(username);
         if (userOpt.isPresent()) {
        	 userRepository.delete(userOpt.get());
         }
    }
    
    public boolean usernameExists(String username) {
        return userRepository.findByUsername(username).isPresent();
    }
    
    public boolean hasUsers() {
        return userRepository.count() > 0;
    }
    
    public void updateUserSettings(String username, Map<String, String> updates) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            Map<String, String> settingsMap = user.getSettings();

            if(settingsMap == null) {
                settingsMap = new HashMap<String,String>();
            } 
            settingsMap.clear();
            settingsMap.putAll(updates);
            user.setSettings(settingsMap);

            userRepository.save(user);
        } 
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public void changeUsername(User user, String newUsername) {
        user.setUsername(newUsername);
        userRepository.save(user);
    }

    public void changePassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
    
    public void changeFirstUse(User user, boolean firstUse) {
        user.setFirstLogin(firstUse);
        userRepository.save(user);
    }
    
    
    public boolean isPasswordCorrect(User user, String currentPassword) {
        return passwordEncoder.matches(currentPassword, user.getPassword());
    }
}
