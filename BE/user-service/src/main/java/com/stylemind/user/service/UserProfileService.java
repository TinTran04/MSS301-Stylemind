package com.stylemind.user.service;

import com.stylemind.user.dto.DeliveryAddressRequest;
import com.stylemind.user.dto.DeliveryAddressResponse;
import com.stylemind.user.dto.UserProfileResponse;

import java.util.List;

public interface UserProfileService {

    UserProfileResponse getProfile(String userId);

    List<DeliveryAddressResponse> getAddresses(String userId);

    DeliveryAddressResponse createAddress(String userId, DeliveryAddressRequest request);

    DeliveryAddressResponse updateAddress(String userId, String addressId, DeliveryAddressRequest request);

    DeliveryAddressResponse setDefaultAddress(String userId, String addressId);

    void deleteAddress(String userId, String addressId);

    DeliveryAddressResponse getAddressById(String userId, String addressId);
}
