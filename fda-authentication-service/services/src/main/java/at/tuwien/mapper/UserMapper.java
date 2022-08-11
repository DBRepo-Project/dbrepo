package at.tuwien.mapper;

import at.tuwien.api.amqp.CreateUserDto;
import at.tuwien.api.amqp.GrantVirtualHostPermissionsDto;
import at.tuwien.api.auth.JwtResponseDto;
import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.user.*;
import at.tuwien.entities.user.RoleType;
import at.tuwien.entities.user.User;
import at.tuwien.exception.OrcidMalformedException;
import org.mapstruct.Mapper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.Objects;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserMapper.class);

    User signupRequestDtoToUser(SignupRequestDto data);

    UserDetailsDto userToUserDetailsDto(User data);

    CreateUserDto signupRequestDtoToCreateUserDto(SignupRequestDto data);

    UserPasswordDto userResetDtoToUserPasswordDto(UserResetDto data);

    @Transactional(readOnly = true)
    default JwtResponseDto principalToJwtResponseDto(Object data) {
        final UserDetailsDto details = (UserDetailsDto) data;
        return JwtResponseDto.builder()
                .id(details.getId())
                .username(details.getUsername())
                .email(details.getEmail())
                .roles(details.getAuthorities()
                        .stream()
                        .filter(Objects::nonNull)
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList()))
                .build();
    }

    @Transactional(readOnly = true)
    default UserDto userToUserDto(User data) throws OrcidMalformedException {
        return UserDto.builder()
                .id(data.getId())
                .username(data.getUsername())
                .email(data.getEmail())
                .password(data.getPassword())
                .firstname(data.getFirstname())
                .lastname(data.getLastname())
                .titlesBefore(data.getTitlesBefore())
                .titlesAfter(data.getTitlesAfter())
                .emailVerified(data.getEmailVerified())
                .affiliation(data.getAffiliation())
                .themeDark(data.getThemeDark())
                .orcid(userToUncompressedOrcid(data))
                .authorities(data.getRoles()
                        .stream()
                        .map(this::roleTypeToGrantedAuthorityDto)
                        .collect(Collectors.toList()))
                .build();
    }

    default GrantedAuthority roleTypeToGrantedAuthority(RoleType data) {
        return new SimpleGrantedAuthority(data.name());
    }

    default GrantedAuthorityDto roleTypeToGrantedAuthorityDto(RoleType data) {
        return GrantedAuthorityDto.builder()
                .authority(data.name())
                .build();
    }

    default GrantedAuthorityDto grantedAuthorityToGrantedAuthority(GrantedAuthority data) {
        return GrantedAuthorityDto.builder()
                .authority(data.getAuthority())
                .build();
    }

    default String userUpdateDtoToCompressedOrcid(UserUpdateDto data) {
        if (data.getOrcid() == null) {
            return null;
        }
        return data.getOrcid().replace("-", "");
    }

    default String userToUncompressedOrcid(User data) throws OrcidMalformedException {
        if (data.getOrcid() == null) {
            return null;
        }
        if (data.getOrcid().length() != 16) {
            log.error("Provided ORCID is not compressed");
            log.debug("provided orcid {} is not compressed, length is {}", data.getOrcid(), data.getOrcid().length());
            throw new OrcidMalformedException("Provided ORCID is not compressed");
        }
        return new StringBuilder(data.getOrcid().substring(0, 4))
                .append("-")
                .append(data.getOrcid(), 4, 8)
                .append("-")
                .append(data.getOrcid(), 8, 12)
                .append("-")
                .append(data.getOrcid(), 12, 16)
                .toString();
    }

    default GrantVirtualHostPermissionsDto signupRequestDtoToGrantComponentDto() {
        return GrantVirtualHostPermissionsDto.builder()
                .virtualHost("/")
                .configure(".*")
                .write(".*")
                .read(".*")
                .build();
    }

    @Transactional(readOnly = true)
    default UserDto userDetailsToUserDto(UserDetails data, Principal principal) {
        final UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
        final UserDto user = UserDto.builder()
                .username(data.getUsername())
                .password(data.getPassword())
                .authorities(token.getAuthorities()
                        .stream()
                        .map(this::grantedAuthorityToGrantedAuthority)
                        .collect(Collectors.toList()))
                .build();
        log.debug("mapped user and principal {}", user);
        return user;
    }

}
