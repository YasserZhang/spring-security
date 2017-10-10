/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.security.config.web.server;

import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.htmlunit.server.WebTestClientHtmlUnitDriverBuilder;
import org.springframework.security.test.web.reactive.server.WebTestClientBuilder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterChainProxy;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Winch
 * @since 5.0
 */
public class FormLoginTests {
	private UserDetails user = User.withUsername("user").password("password").roles("USER").build();
	private ServerHttpSecurity http = ServerHttpSecurity.http();

	ReactiveAuthenticationManager manager = new UserDetailsRepositoryReactiveAuthenticationManager(new MapReactiveUserDetailsService(this.user));

	@Test
	public void defaultLoginPage() {
		SecurityWebFilterChain securityWebFilter = this.http
			.authenticationManager(this.manager)
			.authorizeExchange()
				.anyExchange().authenticated()
				.and()
			.formLogin().and()
			.logout().and()
			.build();

		WebTestClient webTestClient = WebTestClientBuilder
			.bindToWebFilters(securityWebFilter)
			.build();

		WebDriver driver = WebTestClientHtmlUnitDriverBuilder
			.webTestClientSetup(webTestClient)
			.build();

		DefaultLoginPage loginPage = HomePage.to(driver, DefaultLoginPage.class)
			.assertAt();

		loginPage = loginPage.loginForm()
			.username("user")
			.password("invalid")
			.submit(DefaultLoginPage.class)
			.assertError();

		HomePage homePage = loginPage.loginForm()
			.username("user")
			.password("password")
			.submit(HomePage.class);

		homePage.assertAt();

		driver.get("http://localhost/logout");

		DefaultLoginPage.create(driver)
			.assertAt()
			.assertLogout();
	}

	@Test
	public void customLoginPage() {
		SecurityWebFilterChain securityWebFilter = this.http
			.authenticationManager(this.manager)
			.authorizeExchange()
				.pathMatchers("/login").permitAll()
				.anyExchange().authenticated()
				.and()
			.formLogin()
				.loginPage("/login")
				.and()
			.build();

		WebTestClient webTestClient = WebTestClient
			.bindToController(new CustomLoginPageController(), new WebTestClientBuilder.Http200RestController())
			.webFilter(WebFilterChainProxy.fromSecurityWebFilterChains(securityWebFilter))
			.build();

		WebDriver driver = WebTestClientHtmlUnitDriverBuilder
			.webTestClientSetup(webTestClient)
			.build();

		CustomLoginPage loginPage = HomePage.to(driver, CustomLoginPage.class)
			.assertAt();

		HomePage homePage = loginPage.loginForm()
			.username("user")
			.password("password")
			.submit(HomePage.class);

		homePage.assertAt();
	}

	public static class CustomLoginPage {

		private WebDriver driver;

		private LoginForm loginForm;

		public CustomLoginPage(WebDriver webDriver) {
			this.driver = webDriver;
			this.loginForm = PageFactory.initElements(webDriver, LoginForm.class);
		}

		public CustomLoginPage assertAt() {
			assertThat(this.driver.getTitle()).isEqualTo("Custom Log In Page");
			return this;
		}

		public LoginForm loginForm() {
			return this.loginForm;
		}

		public static class LoginForm {
			private WebDriver driver;
			private WebElement username;
			private WebElement password;
			@FindBy(css = "button[type=submit]")
			private WebElement submit;

			public LoginForm(WebDriver driver) {
				this.driver = driver;
			}

			public LoginForm username(String username) {
				this.username.sendKeys(username);
				return this;
			}

			public LoginForm password(String password) {
				this.password.sendKeys(password);
				return this;
			}

			public <T> T submit(Class<T> page) {
				this.submit.click();
				return PageFactory.initElements(this.driver, page);
			}
		}
	}

	public static class DefaultLoginPage {

		private WebDriver driver;
		@FindBy(css = "div[role=alert]")
		private WebElement alert;

		private LoginForm loginForm;

		public DefaultLoginPage(WebDriver webDriver) {
			this.driver = webDriver;
			this.loginForm = PageFactory.initElements(webDriver, LoginForm.class);
		}

		static DefaultLoginPage create(WebDriver driver) {
			return PageFactory.initElements(driver, DefaultLoginPage.class);
		}

		public DefaultLoginPage assertAt() {
			assertThat(this.driver.getTitle()).isEqualTo("Please sign in");
			return this;
		}

		public DefaultLoginPage assertError() {
			assertThat(this.alert.getText()).isEqualTo("Invalid credentials");
			return this;
		}

		public DefaultLoginPage assertLogout() {
			assertThat(this.alert.getText()).isEqualTo("You have been signed out");
			return this;
		}

		public LoginForm loginForm() {
			return this.loginForm;
		}

		public static class LoginForm {
			private WebDriver driver;
			private WebElement username;
			private WebElement password;
			@FindBy(css = "button[type=submit]")
			private WebElement submit;

			public LoginForm(WebDriver driver) {
				this.driver = driver;
			}

			public LoginForm username(String username) {
				this.username.sendKeys(username);
				return this;
			}

			public LoginForm password(String password) {
				this.password.sendKeys(password);
				return this;
			}

			public <T> T submit(Class<T> page) {
				this.submit.click();
				return PageFactory.initElements(this.driver, page);
			}
		}
	}

	public static class HomePage {
		private WebDriver driver;

		public HomePage(WebDriver driver) {
			this.driver = driver;
		}

		public void assertAt() {
			assertThat(this.driver.getPageSource()).contains("ok");
		}

		static <T> T to(WebDriver driver, Class<T> page) {
			driver.get("http://localhost/");
			return PageFactory.initElements(driver, page);
		}
	}

	@Controller
	public static class CustomLoginPageController {
		@ResponseBody
		@GetMapping("/login")
		public String login() {
			return "<!DOCTYPE html>\n"
				+ "<html lang=\"en\">\n"
				+ "  <head>\n"
				+ "    <meta charset=\"utf-8\">\n"
				+ "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1, shrink-to-fit=no\">\n"
				+ "    <meta name=\"description\" content=\"\">\n"
				+ "    <meta name=\"author\" content=\"\">\n"
				+ "    <title>Custom Log In Page</title>\n"
				+ "  </head>\n"
				+ "  <body>\n"
				+ "     <div>\n"
				+ "      <form method=\"post\" action=\"/login\">\n"
				+ "        <h2>Please sign in</h2>\n"
				+ "        <p>\n"
				+ "          <label for=\"username\">Username</label>\n"
				+ "          <input type=\"text\" id=\"username\" name=\"username\" placeholder=\"Username\" required autofocus>\n"
				+ "        </p>\n"
				+ "        <p>\n"
				+ "          <label for=\"password\" class=\"sr-only\">Password</label>\n"
				+ "          <input type=\"password\" id=\"password\" name=\"password\" placeholder=\"Password\" required>\n"
				+ "        </p>\n"
				+ "        <button type=\"submit\">Sign in</button>\n"
				+ "      </form>\n"
				+ "    </div>\n"
				+ "  </body>\n"
				+ "</html>";
		}

	}
}