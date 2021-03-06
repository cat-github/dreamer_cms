package cn.itechyou.blog.security.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;

import javax.servlet.Filter;

import org.apache.shiro.codec.Base64;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.session.SessionListener;
import org.apache.shiro.session.mgt.ExecutorServiceSessionValidationScheduler;
import org.apache.shiro.session.mgt.eis.JavaUuidSessionIdGenerator;
import org.apache.shiro.spring.LifecycleBeanPostProcessor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.CookieRememberMeManager;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.MethodInvokingFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;

import cn.itechyou.blog.security.CustomShiroSessionDAO;
import cn.itechyou.blog.security.cache.JedisManager;
import cn.itechyou.blog.security.cache.JedisShiroSessionRepository;
import cn.itechyou.blog.security.cache.ShiroCacheManager;
import cn.itechyou.blog.security.cache.impl.CustomShiroCacheManager;
import cn.itechyou.blog.security.cache.impl.JedisShiroCacheManager;
import cn.itechyou.blog.security.filter.KickoutSessionFilter;
import cn.itechyou.blog.security.filter.LoginFilter;
import cn.itechyou.blog.security.filter.PermissionFilter;
import cn.itechyou.blog.security.filter.RoleFilter;
import cn.itechyou.blog.security.filter.SimpleAuthFilter;
import cn.itechyou.blog.security.listener.CustomSessionListener;
import cn.itechyou.blog.security.session.CustomSessionManager;
import cn.itechyou.blog.security.token.InteractionRealm;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Configuration
public class ShiroConfiguration {
	
	@Autowired
	private RedisConfiguration redisConfiguration;
	
	/**
	 * ???????????????????????? ????????????????????????????????????????????? eg:/home.jsp = authc,roleOR[admin,user]
	?????????admin??????user?????? ???????????????
	 * @return
	 */
	@Bean("shirFilter")
    public ShiroFilterFactoryBean shirFilter(SecurityManager securityManager) {
        System.out.println("ShiroConfiguration.shirFilter()");
        ShiroFilterFactoryBean shiroFilterFactoryBean = new ShiroFilterFactoryBean();
        shiroFilterFactoryBean.setSecurityManager(securityManager);
        // ????????????????????????????????????Web?????????????????????"/login.jsp"??????
        shiroFilterFactoryBean.setLoginUrl("/u/toLogin");
        //???????????????;
        shiroFilterFactoryBean.setUnauthorizedUrl("/403");
        //????????????
        LinkedHashMap<String,Filter> filters = new LinkedHashMap<>();
        filters.put("login", new LoginFilter());
        filters.put("role", new RoleFilter());
        filters.put("simple", new SimpleAuthFilter());
        filters.put("permission", new PermissionFilter());
        filters.put("kickout", kickoutSessionFilter());
        shiroFilterFactoryBean.setFilters(filters);
        
        LinkedHashMap<String,String> fc = new LinkedHashMap<>();
        /*fc.put("/resource/**", "anon");
        fc.put("/u/**", "anon");
        */
        fc.put("/admin/toLogin", "anon");
        fc.put("/**", "anon");
        fc.put("/logout", "logout");
        fc.put("/admin/**", "kickout,simple,login,permission");
        shiroFilterFactoryBean.setFilterChainDefinitionMap(fc);
        // ?????????????????????????????????
        shiroFilterFactoryBean.setSuccessUrl("/index");
        return shiroFilterFactoryBean;
    }
	
	/**
	 * ??????Cookie??????
	 * @return
	 */
	@Bean("sessionIdCookie")
	public SimpleCookie sessionIdCookie() {
		SimpleCookie simpleCookie = new SimpleCookie("dreamer-blog-s");
		simpleCookie.setHttpOnly(true);
		simpleCookie.setMaxAge(-1);
		return simpleCookie;
	}
	
	/**
	 * ??????????????????????????????????????????
	 * @return
	 */
	@Bean("rememberMeCookie")
	public SimpleCookie rememberMeCookie() {
		SimpleCookie rememberMeCookie = new SimpleCookie("dreamer-blog-r");
		rememberMeCookie.setHttpOnly(true);
		rememberMeCookie.setMaxAge(2592000);
		return rememberMeCookie;
	}
	
	/**
	 * ?????????Session?????????
	 * @return
	 */
	@Bean
	public CustomSessionListener customSessionListener() {
		CustomSessionListener customSessionListener = new CustomSessionListener();
		customSessionListener.setShiroSessionRepository(getJedisShiroSessionRepository());
		return customSessionListener;
	}
	
	/**
	 * rememberMe?????????
	 * @return
	 */
	@Bean("rememberMeManager")
	public CookieRememberMeManager rememberMeManager() {
		CookieRememberMeManager cookieRememberMeManager = new CookieRememberMeManager();
		byte[] cipherKey = Base64.decode("wGiHplamyXlVB11UXWol8g==");
		cookieRememberMeManager.setCipherKey(cipherKey);
		cookieRememberMeManager.setCookie(rememberMeCookie());
		return cookieRememberMeManager;
	}
	
	@Bean("sessionIdGenerator")
	public JavaUuidSessionIdGenerator sessionIdGenerator() {
		return new JavaUuidSessionIdGenerator();
	}
	
	/**
	 * ?????????Shiro Session Dao
	 * @return
	 */
	@Bean("customShiroSessionDAO")
	public CustomShiroSessionDAO customShiroSessionDAO() {
		CustomShiroSessionDAO customShiroSessionDAO = new CustomShiroSessionDAO();
		//??????Session ID?????????
		customShiroSessionDAO.setSessionIdGenerator(sessionIdGenerator());
		customShiroSessionDAO.setShiroSessionRepository(getJedisShiroSessionRepository());
		return customShiroSessionDAO;
	}
	
	/**
	 * ????????????Session?????????Session
	 * @return
	 */
	@Bean("customSessionManager")
	public CustomSessionManager customSessionManager() {
		CustomSessionManager customSessionManager = new CustomSessionManager();
		customSessionManager.setCustomShiroSessionDAO(customShiroSessionDAO());
		customSessionManager.setShiroSessionRepository(getJedisShiroSessionRepository());
		return customSessionManager;
	}
	
	/**
	 * ?????????????????????
	 * @return
	 */
	@Bean
	public ExecutorServiceSessionValidationScheduler executorServiceSessionValidationScheduler() {
		ExecutorServiceSessionValidationScheduler executorServiceSessionValidationScheduler = new ExecutorServiceSessionValidationScheduler();
		executorServiceSessionValidationScheduler.setInterval(18000000);
		executorServiceSessionValidationScheduler.setSessionManager(defaultWebSessionManager());
		return executorServiceSessionValidationScheduler;
	}
	
	/**
	 * ???????????????
	 * @return
	 */
	@Bean("securityManager")
	public DefaultWebSecurityManager defaultWebSecurityManager() {
		DefaultWebSecurityManager defaultWebSecurityManager = new DefaultWebSecurityManager(); 
		defaultWebSecurityManager.setRealm(chinecreditRealm());
		defaultWebSecurityManager.setSessionManager(defaultWebSessionManager());
		defaultWebSecurityManager.setRememberMeManager(rememberMeManager());
		defaultWebSecurityManager.setCacheManager(customShiroCacheManager());
		return defaultWebSecurityManager;
	}

	/**
	 * ????????????
	 * @return
	 */
	@Bean
	public CustomShiroCacheManager customShiroCacheManager() {
		CustomShiroCacheManager cacheManager = new CustomShiroCacheManager();
		cacheManager.setShiroCacheManager(getJedisShiroCacheManager());
		return cacheManager;
	}
	
	/**
	 * redis ??????????????????
	 * @return
	 */
	@Bean("jedisManager")
	public JedisManager getJedisManager() {
		JedisManager jedisManager = new JedisManager();
		JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxIdle(redisConfiguration.getMaxIdle());
        jedisPoolConfig.setMinIdle(redisConfiguration.getMinIdle());
        jedisPoolConfig.setMaxWaitMillis(redisConfiguration.getMaxWaitMillis());

        JedisPool jedisPool = new JedisPool(jedisPoolConfig, redisConfiguration.getHost(), redisConfiguration.getPort(), redisConfiguration.getTimeout());
		jedisManager.setJedisPool(jedisPool);
		return jedisManager;
	}
	
	/**
	 * session ????????????????????????
	 * @return
	 */
	@Bean
	public JedisShiroSessionRepository getJedisShiroSessionRepository() {
		JedisShiroSessionRepository jssr = new JedisShiroSessionRepository();
		jssr.setJedisManager(getJedisManager());
		return jssr;
	}
	
	/**
	 * shiro ??????????????????ShiroCacheManager???????????????redis?????????
	 * @return
	 */
	@Bean
	public ShiroCacheManager getJedisShiroCacheManager() {
		JedisShiroCacheManager cacheManager = new JedisShiroCacheManager();
		cacheManager.setJedisManager(getJedisManager());
		return cacheManager;
	}
	
	/**
	 * ??????????????????????????????SecurityUtils.setSecurityManager(securityManager)
	 * @return
	 */
	@Bean
	public MethodInvokingFactoryBean setDefaultWebSecurityManager() {
		MethodInvokingFactoryBean methodInvokingFactoryBean = new MethodInvokingFactoryBean();
		methodInvokingFactoryBean.setStaticMethod("org.apache.shiro.SecurityUtils.setSecurityManager");
		methodInvokingFactoryBean.setArguments(defaultWebSecurityManager());
		return methodInvokingFactoryBean;
	}
	
	/**
	 * session ????????????????????????????????????
	 * @return
	 */
	@Bean("kickoutSessionFilter")
	public KickoutSessionFilter kickoutSessionFilter() {
		KickoutSessionFilter kickoutSessionFilter = new KickoutSessionFilter();
		kickoutSessionFilter.setKickoutUrl("/u/toLogin?kickout");
		return kickoutSessionFilter;
	}
	
	/**
	 * ????????????jedisShiroSessionRepository
	 * @return
	 */
	@Bean
	public MethodInvokingFactoryBean setJedisShiroSessionRepository() {
		MethodInvokingFactoryBean methodInvokingFactoryBean = new MethodInvokingFactoryBean();
		methodInvokingFactoryBean.setStaticMethod("cn.itechyou.blog.security.filter.KickoutSessionFilter.setShiroSessionRepository");
		methodInvokingFactoryBean.setArguments(getJedisShiroSessionRepository());
		return methodInvokingFactoryBean;
	}
	
	/**
	 * session ?????????
	 * @return
	 */
	@Bean("webSessionManager")
	public DefaultWebSessionManager defaultWebSessionManager() {
		DefaultWebSessionManager defaultWebSessionManager = new DefaultWebSessionManager();
		defaultWebSessionManager.setSessionValidationInterval(1800000);
		defaultWebSessionManager.setGlobalSessionTimeout(1800000);
		defaultWebSessionManager.setSessionDAO(customShiroSessionDAO());
		//session ??????
		List<SessionListener> listeners = new ArrayList<SessionListener>();
		listeners.add(customSessionListener());
		defaultWebSessionManager.setSessionListeners(listeners);
		//???????????? ?????????????????????
		defaultWebSessionManager.setSessionValidationSchedulerEnabled(true);
		//??????????????????????????????????????????
		defaultWebSessionManager.setDeleteInvalidSessions(true);
		//??????Cookie??????
		defaultWebSessionManager.setSessionIdCookie(sessionIdCookie());
		return defaultWebSessionManager;
	}
	
	/**
	 * ?????????Realm
	 * @return
	 */
    @Bean("interactionRealm")
    public InteractionRealm chinecreditRealm(){
    	InteractionRealm realm = new InteractionRealm();
        return realm;
    }
    
    /**
     * Shiro?????????????????????
     * ???????????????static??????????????????redis?????????????????????????????????java.lang.NullPointerException
     * @return
     */
    @Bean
    public static LifecycleBeanPostProcessor lifecycleBeanPostProcessor() {
    	return new LifecycleBeanPostProcessor();
    }
    
    /**
     * Google ???????????????
     */
    @Bean
    public DefaultKaptcha kaptcha() {
    	DefaultKaptcha kaptcha = new DefaultKaptcha();
    	Properties p = new Properties();
    	p.setProperty("kaptcha.border", "no");
    	p.setProperty("kaptcha.textproducer.char.length", "4");
    	p.setProperty("kaptcha.textproducer.font.color", "252,111,180");
    	p.setProperty("kaptcha.image.width", "200");
    	p.setProperty("kaptcha.image.height", "60");
    	Config config = new Config(p);
    	kaptcha.setConfig(config);
    	return kaptcha;
    }
}
