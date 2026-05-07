//package prog3.exam.filter;
//
//import io.github.cdimascio.dotenv.Dotenv;
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.http.MediaType;
//import org.springframework.stereotype.Component;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import java.io.IOException;
//
//@Component
//public class ApiKeyFilter extends OncePerRequestFilter {
//
//    private static final String API_KEY_HEADER = "x-api-key";
//    private static final String VALID_API_KEY = Dotenv.load().get("API_KEY");
//
//    @Override
//    protected void doFilterInternal(HttpServletRequest request,
//                                    HttpServletResponse response,
//                                    FilterChain filterChain)
//            throws ServletException, IOException {
//
//        String apiKey = request.getHeader(API_KEY_HEADER);
//
//        if (apiKey == null || !apiKey.equals(VALID_API_KEY)) {
//            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//            response.setContentType(MediaType.TEXT_PLAIN_VALUE);
//            response.getWriter().write("Bad credentials");
//            return;
//        }
//
//        filterChain.doFilter(request, response);
//    }
//}