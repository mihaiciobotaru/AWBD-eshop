package com.eshop.authservice.controllers;

import com.eshop.authservice.dto.AuthenticationRequest;
import com.eshop.authservice.dto.AuthenticationResponse;
import com.eshop.authservice.dto.UserDto;
import com.eshop.authservice.exception.UserAlreadyExistsException;
import com.eshop.authservice.models.Authority;
import com.eshop.authservice.models.User;
import com.eshop.authservice.service.AuthorityService;
import com.eshop.authservice.service.UserService;
import com.eshop.authservice.security.JwtService;
import com.eshop.authservice.enums.AuthorityEnum;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private AuthorityService authorityService;

    @MockBean
    private PasswordEncoder passwordEncoder; // We need to mock this

    @MockBean
    private AuthenticationManager authenticationManager; // We need to mock this

    @MockBean
    private JwtService jwtService; // We need to mock this

    @Autowired
    private ObjectMapper objectMapper;

    private UserDto registerUserDto;
    private AuthenticationRequest authenticationRequest;
    private User testUser;
    private Authority testAuthority;
    private UserDetails testUserDetails;

    @BeforeEach
    void setUp() {
        // Clear SecurityContext before each test to ensure isolation
        SecurityContextHolder.clearContext();

        // Setup for Registration Tests
        registerUserDto = new UserDto();
        registerUserDto.setUsername("testuser");
        registerUserDto.setEmail("test@example.com");
        registerUserDto.setPassword("Password123!");
        registerUserDto.setMatchingPassword("Password123!");

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("hashedPassword"); // This would be the hashed password

        testAuthority = new Authority(
                "testUser",
                AuthorityEnum.ROLE_USER
        );
        testUser.setAuthority(testAuthority);

        // Setup for Login Tests
        authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setUsername("testuser");
        authenticationRequest.setPassword("Password123!");

        // Mock UserDetails object that AuthenticationManager would return
        testUserDetails = new org.springframework.security.core.userdetails.User(
                testUser.getUsername(),
                testUser.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(testAuthority.getAuthority().name()))
        );
    }

    // --- POST /auth/register ---

    @Test
    void testRegisterUser_success() throws Exception {
        when(userService.registerNewUser(any(UserDto.class))).thenReturn(testUser);
        when(authorityService.createAuthority(any(User.class))).thenReturn(testAuthority);
        when(userService.linkToAuthority(any(User.class), any(Authority.class))).thenReturn(testUser);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerUserDto)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message", is("User registered successfully")));

        verify(userService, times(1)).registerNewUser(any(UserDto.class));
        verify(authorityService, times(1)).createAuthority(any(User.class));
        verify(userService, times(1)).linkToAuthority(any(User.class), any(Authority.class));
    }

    @Test
    void testRegisterUser_passwordMismatch() throws Exception {
        registerUserDto.setMatchingPassword("Mismatch!"); // Deliberate mismatch

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerUserDto)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Validation failed. Please check the 'errors' field for details.")))
                .andExpect(jsonPath("$.errors.matchingPassword[0]", is("Passwords do not match")));

        verify(userService, times(0)).registerNewUser(any(UserDto.class)); // Should not call service
    }

    @Test
    void testRegisterUser_validationError_blankUsername() throws Exception {
        registerUserDto.setUsername(""); // Invalid username

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerUserDto)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Validation failed. Please check the 'errors' field for details.")))
                .andExpect(jsonPath("$.errors.username", notNullValue())); // Expecting error for username
        
        verify(userService, times(0)).registerNewUser(any(UserDto.class));
    }

    @Test
    void testRegisterUser_userAlreadyExists() throws Exception {
        doThrow(new UserAlreadyExistsException("User with this username or email already exists."))
                .when(userService).registerNewUser(any(UserDto.class));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerUserDto)))
                .andDo(print())
                .andExpect(status().isConflict()) // 409 Conflict
                .andExpect(jsonPath("$.message", is("User with this username or email already exists.")));
        
        verify(userService, times(1)).registerNewUser(any(UserDto.class));
    }

    @Test
    void testRegisterUser_internalServerError() throws Exception {
        doThrow(new RuntimeException("Database connection failed."))
                .when(userService).registerNewUser(any(UserDto.class));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerUserDto)))
                .andDo(print())
                .andExpect(status().isInternalServerError()) // 500 Internal Server Error
                .andExpect(jsonPath("$.message", is("An unexpected error occurred during registration.")));

        verify(userService, times(1)).registerNewUser(any(UserDto.class));
    }


    // --- POST /auth/login ---

    @Test
    void testLogin_success() throws Exception {
        // Mock the AuthenticationManager to return a successful Authentication object
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getPrincipal()).thenReturn(testUserDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authenticationMock);

        // Mock JwtService to return a dummy token
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("mocked_jwt_token");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authenticationRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is("mocked_jwt_token")));

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService, times(1)).generateToken(any(UserDetails.class));
    }

    @Test
    void testLogin_badCredentials() throws Exception {
        // Mock AuthenticationManager to throw BadCredentialsException
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authenticationRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized()) // 401 Unauthorized
                .andExpect(jsonPath("$.message", is("Invalid username or password.")));

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService, times(0)).generateToken(any(UserDetails.class)); // Token should not be generated
    }

    @Test
    void testLogin_internalServerError() throws Exception {
        // Mock AuthenticationManager to throw a generic exception
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new RuntimeException("Something unexpected happened during authentication"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authenticationRequest)))
                .andDo(print())
                .andExpect(status().isInternalServerError()) // 500 Internal Server Error
                .andExpect(jsonPath("$.message", is("An unexpected error occurred during login.")));

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService, times(0)).generateToken(any(UserDetails.class));
    }


    // --- GET /auth/me ---

    @Test
    void testGetAuthenticatedUserInfo_success() throws Exception {
        // Simulate an authenticated user by setting SecurityContextHolder
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.isAuthenticated()).thenReturn(true);
        when(authenticationMock.getName()).thenReturn(testUser.getUsername());

        SecurityContextHolder.getContext().setAuthentication(authenticationMock);

        // Mock userService to return the user
        when(userService.findByUsername(testUser.getUsername())).thenReturn(Optional.of(testUser));

        mockMvc.perform(get("/auth/me"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is(testUser.getUsername())))
                .andExpect(jsonPath("$.email", is(testUser.getEmail())))
                .andExpect(jsonPath("$.password").doesNotExist()); // Ensure password hash is not leaked (if returning a DTO)

        verify(userService, times(1)).findByUsername(testUser.getUsername());
    }

    @Test
    void testGetAuthenticatedUserInfo_notAuthenticated() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("User not authenticated.")));

        verify(userService, times(0)).findByUsername(any(String.class));
    }

    @Test
    void testGetAuthenticatedUserInfo_userNotFoundAfterAuthentication() throws Exception {
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.isAuthenticated()).thenReturn(true);
        when(authenticationMock.getName()).thenReturn("nonexistentuser"); // Authenticated, but user not in DB
        SecurityContextHolder.getContext().setAuthentication(authenticationMock);

        // Mock userService to return empty (user not found)
        when(userService.findByUsername("nonexistentuser")).thenReturn(Optional.empty());

        mockMvc.perform(get("/auth/me"))
                .andDo(print())
                .andExpect(status().isInternalServerError()) // Controller returns 500 for this case
                .andExpect(jsonPath("$.message", is("Error fetching user info.")));

        verify(userService, times(1)).findByUsername("nonexistentuser");
    }
}