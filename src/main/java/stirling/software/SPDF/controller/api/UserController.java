package stirling.software.SPDF.controller.api;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import stirling.software.SPDF.config.security.UserService;
import stirling.software.SPDF.model.User;

@Controller
@RequestMapping("/api/v1/user")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @PostMapping("/register")
    public String register(@RequestParam String username, @RequestParam String password, Model model) {
        if(userService.usernameExists(username)) {
            model.addAttribute("error", "Username already exists");
            return "register";
        }

        userService.saveUser(username, password);
        return "redirect:/login?registered=true";
    }
    
    @PostMapping("/change-username-and-password")
    public RedirectView changeUsernameAndPassword(Principal principal,
                                                 @RequestParam String currentPassword, 
                                                 @RequestParam String newUsername, 
                                                 @RequestParam String newPassword, 
                                                 HttpServletRequest request, 
                                                 HttpServletResponse response,
                                                 RedirectAttributes redirectAttributes) {
    	if (principal == null) {
    	    return new RedirectView("/change-creds?messageType=notAuthenticated");
    	}

    	Optional<User> userOpt = userService.findByUsername(principal.getName());

    	if (userOpt == null || userOpt.isEmpty()) {
    	    return new RedirectView("/change-creds?messageType=userNotFound");
    	}

    	User user = userOpt.get();

    	if (!userService.isPasswordCorrect(user, currentPassword)) {
    	    return new RedirectView("/change-creds?messageType=incorrectPassword");
    	}

    	if (!user.getUsername().equals(newUsername) && userService.usernameExists(newUsername)) {
    	    return new RedirectView("/change-creds?messageType=usernameExists");
    	}


        userService.changePassword(user, newPassword);
        if(newUsername != null && newUsername.length() > 0 && !user.getUsername().equals(newUsername)) {
            userService.changeUsername(user, newUsername);
        }
        userService.changeFirstUse(user, false);

        // Logout using Spring's utility
        new SecurityContextLogoutHandler().logout(request, response, null);

        return new RedirectView("/login?messageType=credsUpdated");
    }


    
    @PostMapping("/change-username")
    public RedirectView changeUsername(Principal principal,
                                       @RequestParam String currentPassword, 
                                       @RequestParam String newUsername, 
                                       HttpServletRequest request, 
                                       HttpServletResponse response,
                                       RedirectAttributes redirectAttributes) {
    	if (principal == null) {
    	    return new RedirectView("/account?messageType=notAuthenticated");
    	}

    	Optional<User> userOpt = userService.findByUsername(principal.getName());

    	if (userOpt == null || userOpt.isEmpty()) {
    	    return new RedirectView("/account?messageType=userNotFound");
    	}

    	User user = userOpt.get();

    	if (!userService.isPasswordCorrect(user, currentPassword)) {
    	    return new RedirectView("/account?messageType=incorrectPassword");
    	}

    	if (!user.getUsername().equals(newUsername) && userService.usernameExists(newUsername)) {
    	    return new RedirectView("/account?messageType=usernameExists");
    	}

    	if(newUsername != null && newUsername.length() > 0) {
            userService.changeUsername(user, newUsername);
        }

        // Logout using Spring's utility
        new SecurityContextLogoutHandler().logout(request, response, null);

        return new RedirectView("/login?messageType=credsUpdated");
    }

    @PostMapping("/change-password")
    public RedirectView changePassword(Principal principal, 
                                       @RequestParam String currentPassword, 
                                       @RequestParam String newPassword, 
                                       HttpServletRequest request, 
                                       HttpServletResponse response,
                                       RedirectAttributes redirectAttributes) {
    	if (principal == null) {
    	    return new RedirectView("/account?messageType=notAuthenticated");
    	}

    	Optional<User> userOpt = userService.findByUsername(principal.getName());

    	if (userOpt == null || userOpt.isEmpty()) {
    	    return new RedirectView("/account?messageType=userNotFound");
    	}

    	User user = userOpt.get();

    	if (!userService.isPasswordCorrect(user, currentPassword)) {
    	    return new RedirectView("/account?messageType=incorrectPassword");
    	}

        userService.changePassword(user, newPassword);

        // Logout using Spring's utility
        new SecurityContextLogoutHandler().logout(request, response, null);

        return new RedirectView("/login?messageType=credsUpdated");
    }

    
    @PostMapping("/updateUserSettings")
	public String updateUserSettings(HttpServletRequest request, Principal principal) {
	    Map<String, String[]> paramMap = request.getParameterMap();
	    Map<String, String> updates = new HashMap<>();

	    System.out.println("Received parameter map: " + paramMap);

	    for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
	        updates.put(entry.getKey(), entry.getValue()[0]);
	    }

	    System.out.println("Processed updates: " + updates);

	    // Assuming you have a method in userService to update the settings for a user
	    userService.updateUserSettings(principal.getName(), updates);

	    return "redirect:/account";  // Redirect to a page of your choice after updating
	}

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/admin/saveUser")
    public RedirectView saveUser(@RequestParam String username, @RequestParam String password, @RequestParam String role, 
    		@RequestParam(name = "forceChange", required = false, defaultValue = "false") boolean forceChange) {
    	
    	if(userService.usernameExists(username)) {
    		return new RedirectView("/addUsers?messageType=usernameExists");
    	}
        userService.saveUser(username, password, role, forceChange);
        return new RedirectView("/addUsers");  // Redirect to account page after adding the user
    }

    
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/admin/deleteUser/{username}")
    public String deleteUser(@PathVariable String username,  Authentication authentication) {
    	
    	// Get the currently authenticated username
        String currentUsername = authentication.getName();

        // Check if the provided username matches the current session's username
        if (currentUsername.equals(username)) {
            throw new IllegalArgumentException("Cannot delete currently logined in user.");
        }
        
    	userService.deleteUser(username); 
        return "redirect:/addUsers";
    }
    
    @PostMapping("/get-api-key")
    public ResponseEntity<String> getApiKey(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User not authenticated.");
        }
        String username = principal.getName();
        String apiKey = userService.getApiKeyForUser(username);
        if (apiKey == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("API key not found for user.");
        }
        return ResponseEntity.ok(apiKey);
    }

    @PostMapping("/update-api-key")
    public ResponseEntity<String> updateApiKey(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User not authenticated.");
        }
        String username = principal.getName();
        User user = userService.refreshApiKeyForUser(username);
        String apiKey = user.getApiKey();
        if (apiKey == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("API key not found for user.");
        }
        return ResponseEntity.ok(apiKey);
    }
    
    
}
