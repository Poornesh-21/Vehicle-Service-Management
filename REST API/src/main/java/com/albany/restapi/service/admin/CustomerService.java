package com.albany.restapi.service.admin;

import com.albany.restapi.dto.CustomerDTO;
import com.albany.restapi.model.CustomerProfile;
import com.albany.restapi.model.User;
import com.albany.restapi.repository.CustomerRepository;
import com.albany.restapi.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<CustomerDTO> getAllCustomers() {
        return customerRepository.findAllActiveCustomers().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public CustomerDTO getCustomerById(Integer customerId) {
        CustomerProfile customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found with id: " + customerId));
        return mapToDTO(customer);
    }

    @Transactional
    public CustomerDTO createCustomer(CustomerDTO customerDTO) {
        User user = User.builder()
                .email(customerDTO.getEmail())
                .password(passwordEncoder.encode("changeme"))
                .firstName(customerDTO.getFirstName())
                .lastName(customerDTO.getLastName())
                .phoneNumber(customerDTO.getPhoneNumber())
                .role(User.Role.customer)
                .isActive(true)
                .street(customerDTO.getStreet())
                .city(customerDTO.getCity())
                .state(customerDTO.getState())
                .postalCode(customerDTO.getPostalCode())
                .membershipType(customerDTO.getMembershipStatus().equals("Premium") ?
                        User.MembershipType.PREMIUM : User.MembershipType.STANDARD)
                .build();

        User savedUser = userRepository.save(user);

        CustomerProfile customerProfile = CustomerProfile.builder()
                .user(savedUser)
                .street(customerDTO.getStreet())
                .city(customerDTO.getCity())
                .state(customerDTO.getState())
                .postalCode(customerDTO.getPostalCode())
                .membershipStatus(customerDTO.getMembershipStatus())
                .totalServices(0)
                .build();

        CustomerProfile savedCustomer = customerRepository.save(customerProfile);
        return mapToDTO(savedCustomer);
    }

    @Transactional
    public CustomerDTO updateCustomer(Integer customerId, CustomerDTO customerDTO) {
        CustomerProfile existingCustomer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found with id: " + customerId));

        User existingUser = existingCustomer.getUser();
        existingUser.setFirstName(customerDTO.getFirstName());
        existingUser.setLastName(customerDTO.getLastName());
        existingUser.setEmail(customerDTO.getEmail());
        existingUser.setPhoneNumber(customerDTO.getPhoneNumber());
        existingUser.setStreet(customerDTO.getStreet());
        existingUser.setCity(customerDTO.getCity());
        existingUser.setState(customerDTO.getState());
        existingUser.setPostalCode(customerDTO.getPostalCode());
        existingUser.setMembershipType(customerDTO.getMembershipStatus().equals("Premium") ?
                User.MembershipType.PREMIUM : User.MembershipType.STANDARD);

        userRepository.save(existingUser);

        existingCustomer.setStreet(customerDTO.getStreet());
        existingCustomer.setCity(customerDTO.getCity());
        existingCustomer.setState(customerDTO.getState());
        existingCustomer.setPostalCode(customerDTO.getPostalCode());
        existingCustomer.setMembershipStatus(customerDTO.getMembershipStatus());

        CustomerProfile updatedCustomer = customerRepository.save(existingCustomer);
        return mapToDTO(updatedCustomer);
    }

    private CustomerDTO mapToDTO(CustomerProfile customerProfile) {
        User user = customerProfile.getUser();
        return CustomerDTO.builder()
                .customerId(customerProfile.getCustomerId())
                .userId(user.getUserId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .street(customerProfile.getStreet() != null ? customerProfile.getStreet() : user.getStreet())
                .city(customerProfile.getCity() != null ? customerProfile.getCity() : user.getCity())
                .state(customerProfile.getState() != null ? customerProfile.getState() : user.getState())
                .postalCode(customerProfile.getPostalCode() != null ? customerProfile.getPostalCode() : user.getPostalCode())
                .membershipStatus(customerProfile.getMembershipStatus())
                .totalServices(customerProfile.getTotalServices())
                .lastServiceDate(customerProfile.getLastServiceDate())
                .isActive(user.isActive())
                .build();
    }
}