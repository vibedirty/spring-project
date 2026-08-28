package com.cat.hard.account.address.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cat.hard.account.address.dto.AddressUpdateRequest;
import com.cat.hard.account.address.entity.UserAddress;
import com.cat.hard.account.address.service.AddressService;
import com.cat.hard.account.auth.jwt.JwtSessionService;
import com.cat.hard.account.auth.jwt.JwtTokenProvider;
import com.cat.hard.account.user.enums.UserRole;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AddressControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private JwtSessionService jwtSessionService;

	@MockitoBean
	private AddressService addressService;

	@BeforeEach
	void setUp() {
		when(jwtSessionService.isActive(any(), any())).thenReturn(true);
	}

	@Test
	void shouldAllowUserToUpdateAddress() throws Exception {
		UserAddress address = address();
		when(addressService.update(
				eq(10L),
				org.mockito.ArgumentMatchers.any(AddressUpdateRequest.class)))
				.thenReturn(address);
		String token = jwtTokenProvider.generateToken(7L, UserRole.USER);

		mockMvc.perform(post("/api/addresses/10/update")
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "receiverName": "李四",
							  "phone": "13900000000",
							  "province": "上海市",
							  "city": "上海市",
							  "district": "静安区",
							  "detailAddress": "静安区 2 号",
							  "isDefault": 0
							}
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data.id").value(10))
				.andExpect(jsonPath("$.data.userId").value(7))
				.andExpect(jsonPath("$.data.receiverName").value("李四"))
				.andExpect(jsonPath("$.data.isDefault").value(0));

		verify(addressService).update(
				eq(10L),
				argThat(request -> "李四".equals(request.getReceiverName())
						&& "13900000000".equals(request.getPhone())
						&& Integer.valueOf(0).equals(request.getIsDefault())));
	}

	@Test
	void shouldAllowUserToSetDefaultAddress() throws Exception {
		UserAddress address = address();
		address.setIsDefault(1);
		when(addressService.setDefault(10L)).thenReturn(address);
		String token = jwtTokenProvider.generateToken(7L, UserRole.USER);

		mockMvc.perform(post("/api/addresses/10/set-default")
					.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data.id").value(10))
				.andExpect(jsonPath("$.data.userId").value(7))
				.andExpect(jsonPath("$.data.isDefault").value(1));

		verify(addressService).setDefault(10L);
	}

	@Test
	void shouldAllowUserToDeleteAddress() throws Exception {
		String token = jwtTokenProvider.generateToken(7L, UserRole.USER);

		mockMvc.perform(post("/api/addresses/10/delete")
					.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.message").value("success"))
				.andExpect(jsonPath("$.data").doesNotExist());

		verify(addressService).delete(10L);
	}

	private UserAddress address() {
		UserAddress address = new UserAddress();
		address.setId(10L);
		address.setUserId(7L);
		address.setReceiverName("李四");
		address.setPhone("13900000000");
		address.setProvince("上海市");
		address.setCity("上海市");
		address.setDistrict("静安区");
		address.setDetailAddress("静安区 2 号");
		address.setIsDefault(0);
		return address;
	}
}
