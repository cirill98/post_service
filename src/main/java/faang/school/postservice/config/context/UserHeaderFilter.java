package faang.school.postservice.config.context;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class UserHeaderFilter implements Filter {

    private final UserContext userContext;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String userIdHeader = req.getHeader("x-user-id");
        if (userIdHeader != null) {
            try {
                long userId = Long.parseLong(userIdHeader);
                userContext.setUserId(userId);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid 'x-user-id' header value. Must be a valid long integer.", e);
            }
        } else {
            throw new IllegalArgumentException("Missing required header 'x-user-id'. Please include 'x-user-id' header with a valid user ID in your request.");
        }
        try {
            chain.doFilter(request, response);
        } finally {
            userContext.clear();
        }
    }
}
