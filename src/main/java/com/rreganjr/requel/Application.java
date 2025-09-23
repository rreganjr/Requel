/*
 * $Id: CreatedEntity.java 124 2009-05-21 23:46:02Z rreganjr $
 *
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 *
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Requel is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Requel is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Requel. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.rreganjr.requel;


import net.sf.echopm.EchoPMLogoutServlet;
import net.sf.echopm.EchoPMServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@SpringBootApplication
@ServletComponentScan
@EntityScan( basePackages = {"com.rreganjr.requel", "com.rreganjr.nlp"} )
@ImportResource( {"classpath:application-config.xml"} )
public class Application extends SpringBootServletInitializer {

    @Bean
    ServletRegistrationBean initEchoPMServlet() {
        ServletRegistrationBean echoPMServletRegisterBean = new ServletRegistrationBean();
        echoPMServletRegisterBean.setLoadOnStartup(1);
        echoPMServletRegisterBean.addUrlMappings("/");
        echoPMServletRegisterBean.setServlet(new EchoPMServlet());
        return echoPMServletRegisterBean;
    }

    @Bean
    ServletRegistrationBean initEchoPMLogoutServlet() {
        ServletRegistrationBean echoPMServletRegisterBean = new ServletRegistrationBean();
        echoPMServletRegisterBean.setLoadOnStartup(1);
        echoPMServletRegisterBean.addUrlMappings("/logout");
        echoPMServletRegisterBean.setServlet(new EchoPMLogoutServlet());
        return echoPMServletRegisterBean;
    }

    @Configuration
    static class WebSecurityConfig extends WebSecurityConfigurerAdapter {
        @Override
        protected void configure(HttpSecurity http) throws Exception {
            //http://www.codesandnotes.be/2014/10/31/restful-authentication-using-spring-security-on-spring-boot-and-jquery-as-a-web-client/

            http
                    .httpBasic()
                    .and().headers().frameOptions().sameOrigin()
                    .and().authorizeRequests().antMatchers("/**").anonymous()
                    .and().csrf().disable();
        }


        @Autowired
        public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
            auth
                    .inMemoryAuthentication()
                    .withUser("admin")
                    .password("{noop}admin")
                    .roles("ADMIN","USER");
        }
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(Application.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
