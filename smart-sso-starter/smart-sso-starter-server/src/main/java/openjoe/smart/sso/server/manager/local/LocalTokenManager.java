package openjoe.smart.sso.server.manager.local;

import openjoe.smart.sso.base.entity.ExpirationPolicy;
import openjoe.smart.sso.base.entity.ExpirationWrapper;
import openjoe.smart.sso.server.entity.TokenContent;
import openjoe.smart.sso.server.manager.AbstractTokenManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地调用凭证管理
 *
 * @author Joe
 */
public class LocalTokenManager extends AbstractTokenManager implements ExpirationPolicy {

    private final Logger logger = LoggerFactory.getLogger(LocalTokenManager.class);
    private Map<String, ExpirationWrapper<TokenContent>> refreshTokenMap = new ConcurrentHashMap<>();
    private Map<String, Set<String>> tgtMap = new ConcurrentHashMap<>();

    public LocalTokenManager(int accessTokenTimeout, int refreshTokenTimeout) {
        super(accessTokenTimeout, refreshTokenTimeout);
    }

    @Override
    public void create(String refreshToken, TokenContent tokenContent) {
        ExpirationWrapper<TokenContent> dat = new ExpirationWrapper(tokenContent, getRefreshTokenTimeout());
        refreshTokenMap.put(refreshToken, dat);

        tgtMap.computeIfAbsent(tokenContent.getTgt(), a -> new HashSet<>()).add(refreshToken);
        logger.debug("调用凭证创建成功, accessToken:{}, refreshToken:{}", tokenContent.getAccessToken(), refreshToken);
    }

    @Override
    public TokenContent get(String refreshToken) {
        ExpirationWrapper<TokenContent> wrapper = refreshTokenMap.get(refreshToken);
        if (wrapper == null || wrapper.checkExpired()) {
            return null;
        } else {
            return wrapper.getObject();
        }
    }

    @Override
    public void remove(String refreshToken) {
        ExpirationWrapper<TokenContent> wrapper = refreshTokenMap.remove(refreshToken);
        if (wrapper == null) {
            return;
        }
        Set<String> tokenSet = tgtMap.get(wrapper.getObject().getTgt());
        if (CollectionUtils.isEmpty(tokenSet)) {
            return;
        }
        tokenSet.remove(refreshToken);
    }

    @Override
    public void removeByTgt(String tgt) {
        // 删除所有tgt对应的凭证
        Set<String> refreshTokenSet = tgtMap.remove(tgt);
        if (CollectionUtils.isEmpty(refreshTokenSet)) {
            return;
        }
        // 通知所有客户端退出，并注销本地Token
        refreshTokenSet.forEach(refreshToken -> {
            ExpirationWrapper<TokenContent> wrapper = refreshTokenMap.remove(refreshToken);
            if (wrapper == null) {
                return;
            }
            TokenContent tokenContent = wrapper.getObject();
            if (tokenContent == null) {
                return;
            }
            logger.debug("发起客户端退出请求, accessToken:{}, refreshToken:{}, url:{}", tokenContent.getAccessToken(), refreshToken, tokenContent.getRedirectUri());
            sendLogoutRequest(tokenContent.getRedirectUri(), tokenContent.getAccessToken());
        });
    }

    @Override
    public void verifyExpired() {
        refreshTokenMap.forEach((refreshToken, wrapper) -> {
            if (wrapper.checkExpired()) {
                remove(refreshToken);
                logger.debug("调用凭证已失效, accessToken:{}, refreshToken:{}", wrapper.getObject().getAccessToken(), refreshToken);
            }
        });
    }
}
