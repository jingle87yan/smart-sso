package openjoe.smart.sso.client.filter;

import openjoe.smart.sso.base.constant.BaseConstant;
import openjoe.smart.sso.client.ClientProperties;
import openjoe.smart.sso.client.token.TokenStorage;
import org.springframework.core.annotation.Order;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 单点退出Filter
 *
 * @author Joe
 */
@Order(10)
public class LogoutFilter extends AbstractClientFilter {

    private ClientProperties properties;
    private TokenStorage tokenStorage;

    public LogoutFilter(ClientProperties properties, TokenStorage tokenStorage) {
        this.properties = properties;
        this.tokenStorage = tokenStorage;
    }

    @Override
    public boolean isAccessAllowed(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getServletPath();
        if (!properties.getLogoutPath().equals(path)){
            return true;
        }
        String accessToken = getLogoutParam(request);
        if (accessToken == null) {
            return true;
        }
        getTokenStorage().remove(accessToken);
        return false;
    }

    private String getLogoutParam(HttpServletRequest request) {
        return request.getHeader(BaseConstant.LOGOUT_PARAMETER_NAME);
    }

    public TokenStorage getTokenStorage() {
        return tokenStorage;
    }

    public void setTokenStorage(TokenStorage tokenStorage) {
        this.tokenStorage = tokenStorage;
    }
}