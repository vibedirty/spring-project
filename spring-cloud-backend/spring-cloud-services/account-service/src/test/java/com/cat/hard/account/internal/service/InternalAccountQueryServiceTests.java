package com.cat.hard.account.internal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.cat.hard.account.address.entity.UserAddress;
import com.cat.hard.account.address.service.AddressService;
import com.cat.hard.account.internal.config.InternalApiSimulationProperties;
import com.cat.hard.account.internal.dto.AddressSnapshot;
import com.cat.hard.account.internal.dto.UserSummary;
import com.cat.hard.account.user.entity.User;
import com.cat.hard.account.user.enums.UserRole;
import com.cat.hard.account.user.enums.UserStatus;
import com.cat.hard.account.user.mapper.UserMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InternalAccountQueryServiceTests {

	@Mock
	private UserMapper userMapper;

	@Mock
	private AddressService addressService;

	@Spy
	private InternalApiSimulationProperties simulationProperties =
			new InternalApiSimulationProperties();

	@InjectMocks
	private InternalAccountQueryService internalAccountQueryService;

	@Test
	void shouldReturnUserSummaryWithoutExposingEntity() {
		User user = new User();
		user.setId(7L);
		user.setUsername("user7");
		user.setNickname("用户7");
		user.setRole(UserRole.USER);
		user.setStatus(UserStatus.ENABLED);
		when(userMapper.selectById(7L)).thenReturn(user);

		UserSummary summary = internalAccountQueryService.getUserSummary(7L);

		assertThat(summary).isEqualTo(new UserSummary(
				7L, "user7", "用户7", "USER", "ENABLED"));
	}

	@Test
	void shouldRequireAddressOwnershipAndReturnSnapshot() {
		UserAddress address = new UserAddress();
		address.setId(10L);
		address.setUserId(7L);
		address.setReceiverName("张三");
		address.setPhone("13800138000");
		address.setProvince("广东省");
		address.setCity("深圳市");
		address.setDistrict("南山区");
		address.setDetailAddress("科技园1号");
		when(addressService.getOwnedAddressForUser(7L, 10L)).thenReturn(address);

		AddressSnapshot snapshot = internalAccountQueryService
				.getAddressSnapshot(7L, 10L);

		assertThat(snapshot.userId()).isEqualTo(7L);
		assertThat(snapshot.addressId()).isEqualTo(10L);
		assertThat(snapshot.receiverName()).isEqualTo("张三");
	}
}
