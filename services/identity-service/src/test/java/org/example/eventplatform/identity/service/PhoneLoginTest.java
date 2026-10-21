package org.example.eventplatform.identity.service;

import org.example.eventplatform.identity.client.AccountCleanupClient;
import org.example.eventplatform.identity.dto.auth.LoginRequest;
import org.example.eventplatform.identity.entity.Role;
import org.example.eventplatform.identity.entity.Tenant;
import org.example.eventplatform.identity.entity.User;
import org.example.eventplatform.identity.entity.UserStatus;
import org.example.eventplatform.identity.repository.RoleRepository;
import org.example.eventplatform.identity.repository.TenantRepository;
import org.example.eventplatform.identity.repository.UserRepository;
import org.example.eventplatform.shared.exception.ApiException;
import org.example.eventplatform.shared.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PhoneLoginTest {

    private final UserRepository users = mock(UserRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private AuthService auth;
    private PhoneAccounts phones;

    @BeforeEach
    void setUp() {
        phones = new PhoneAccounts(users);
        JwtTokenProvider jwt = mock(JwtTokenProvider.class);
        when(jwt.generateToken(any())).thenReturn("access");
        when(jwt.generateRefreshToken(any())).thenReturn("refresh");
        auth = new AuthService(users, mock(TenantRepository.class), mock(RoleRepository.class), jwt, encoder,
                mock(AccountCleanupClient.class), phones);
    }

    private User user(long id, String role, String pw, Tenant tenant) {
        Role r = new Role();
        r.setName(role);
        return User.builder().id(id).username("u" + id).phone("0900000001").password(encoder.encode(pw))
                .roles(r).tenant(tenant).isActive(true).status(UserStatus.ACTIVE).build();
    }

    private static LoginRequest byPhone(String phone, String pw) {
        LoginRequest r = new LoginRequest();
        r.setPhone(phone);
        r.setPassword(pw);
        return r;
    }

    @Test
    void chuanHoaSoDienThoai() {
        assertThat(PhoneAccounts.normalize("+84 900 000 001")).isEqualTo("0900000001");
        assertThat(PhoneAccounts.normalize("84900000001")).isEqualTo("0900000001");
        assertThat(PhoneAccounts.normalize("0900.000.001")).isEqualTo("0900000001");
        assertThat(PhoneAccounts.normalize("  ")).isNull();
        assertThatThrownBy(() -> PhoneAccounts.normalize("12345")).isInstanceOf(ApiException.class);
    }

    @Test
    void dangNhapBangSdtKhongCanMaDoan() {
        Tenant t = new Tenant();
        t.setId(9L);
        when(users.findAllByPhoneOrCustomerUsername("0900000001")).thenReturn(List.of(user(12, "TN_MEMBER", "secret1", t)));
        var res = auth.login(byPhone("+84900000001", "secret1"));
        assertThat(res.getUser().getRoleName()).isEqualTo("TN_MEMBER");
        assertThat(res.getUser().getTenantId()).isEqualTo(9L);
    }

    @Test
    void saiMatKhauHoacKhongCoTaiKhoanBaoChungMotLoi() {
        when(users.findAllByPhoneOrCustomerUsername("0900000001")).thenReturn(List.of(user(13, "CUSTOMER", "secret1", null)));
        assertThatThrownBy(() -> auth.login(byPhone("0900000001", "sai"))).isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "INVALID_CREDENTIALS");
        assertThatThrownBy(() -> auth.login(byPhone("abc", "secret1"))).isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "INVALID_CREDENTIALS");
    }

    @Test
    void taiKhoanDaXoaKhongDangNhapDuoc() {
        User deleted = user(13, "CUSTOMER", "secret1", null);
        deleted.setStatus(UserStatus.DELETED);
        when(users.findAllByPhoneOrCustomerUsername("0900000001")).thenReturn(List.of(deleted));
        assertThatThrownBy(() -> auth.login(byPhone("0900000001", "secret1"))).isInstanceOf(ApiException.class);
    }

    @Test
    void sdtDaDungChoTaiKhoanKhacThiBaoTrung() {
        when(users.findAllByPhoneOrCustomerUsername("0900000001")).thenReturn(List.of(user(12, "TN_MEMBER", "x", null)));
        assertThatThrownBy(() -> phones.ensureFree("0900000001", null)).hasFieldOrPropertyWithValue("code", "PHONE_TAKEN");
        phones.ensureFree("0900000001", 12L);
    }

    @Test
    void cachCuMaDoanVaUsernameVanCanUsername() {
        LoginRequest r = new LoginRequest();
        r.setPassword("x");
        assertThatThrownBy(() -> auth.login(r)).hasFieldOrPropertyWithValue("code", "LOGIN_ID_REQUIRED");
    }
}
