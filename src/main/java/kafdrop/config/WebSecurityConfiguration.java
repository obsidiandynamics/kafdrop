package kafdrop.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
@ConfigurationProperties(prefix = "spring.security.user")
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(WebSecurityConfiguration.class);

    private String name;
    private String password;
    private String roles;

    // public setters required for property initialization
    public void setName(String name) { this.name = name; }
    public void setPassword(String password) { this.password = password; }
    public void setRoles(String roles) { this.roles = roles; }


    // allow access to /login. Require authentication for all other pages.
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .authorizeRequests()
                .antMatchers("/login*").permitAll()
                .anyRequest().authenticated()
              .and()
                .formLogin()
                .loginPage("/login")
                .permitAll();

    }

    // authenticate user
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        LOG.info(String.format("authenticating user with name: %s password: %s role: %s",
                this.name, this.password, this.roles));
        auth.inMemoryAuthentication()
                .passwordEncoder(passwordEncoder())
                .withUser(this.name)
                .password(passwordEncoder().encode(this.password))
                .roles(this.roles);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // don't enforce access to these static resources
    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/resources/**", "/static/**", "/css/**", "/js/**", "/images/**");
    }
}
