package com.cat.hard.address.controller;

import java.util.ArrayList;
import java.util.List;

import com.cat.hard.address.dto.AddressCreateRequest;
import com.cat.hard.address.dto.AddressResponse;
import com.cat.hard.address.dto.AddressUpdateRequest;
import com.cat.hard.address.entity.UserAddress;
import com.cat.hard.address.service.AddressService;
import com.cat.hard.common.api.ApiResponse;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {

	@Resource
	private AddressService addressService;

	@GetMapping
	public ApiResponse<List<AddressResponse>> list() {
		List<UserAddress> addresses = addressService.list();
		List<AddressResponse> response = new ArrayList<AddressResponse>();
		for (UserAddress address : addresses) {
			response.add(AddressResponse.from(address));
		}
		return ApiResponse.success(response);
	}

	@PostMapping
	public ApiResponse<AddressResponse> create(
			@Valid @RequestBody AddressCreateRequest request) {
		UserAddress address = addressService.create(request);
		return ApiResponse.success(AddressResponse.from(address));
	}

	@PostMapping("/{id}/update")
	public ApiResponse<AddressResponse> update(
			@PathVariable Long id,
			@Valid @RequestBody AddressUpdateRequest request) {
		UserAddress address = addressService.update(id, request);
		return ApiResponse.success(AddressResponse.from(address));
	}

	@PostMapping("/{id}/set-default")
	public ApiResponse<AddressResponse> setDefault(@PathVariable Long id) {
		UserAddress address = addressService.setDefault(id);
		return ApiResponse.success(AddressResponse.from(address));
	}

	@PostMapping("/{id}/delete")
	public ApiResponse<Void> delete(@PathVariable Long id) {
		addressService.delete(id);
		return ApiResponse.success();
	}
}
