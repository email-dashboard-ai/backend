package org.example.model;

import jakarta.persistence.*;
import java.util.Collection;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.enums.AuthProvider;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class User implements UserDetails {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String email;

  private String password;

  private String name;

  private AuthProvider provider;

  private String avatar;

  @Column(length = 2048) // Tokens can be long
  private String googleAccessToken;

  @Column(length = 2048)
  private String googleRefreshToken;

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of();
  }

  @Override
  public String getUsername() {
    return email;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }
}
