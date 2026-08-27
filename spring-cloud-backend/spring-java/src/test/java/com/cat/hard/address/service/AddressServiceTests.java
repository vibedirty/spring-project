package com.cat.hard.address.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.cat.hard.address.dto.AddressCreateRequest;
import com.cat.hard.address.dto.AddressUpdateRequest;
import com.cat.hard.address.entity.UserAddress;
import com.cat.hard.address.mapper.UserAddressMapper;
import com.cat.hard.auth.security.CurrentUser;
import com.cat.hard.common.error.ErrorCode;
import com.cat.hard.common.exception.BusinessException;

import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
class AddressServiceTests {

	@BeforeAll
	static void initializeUserAddressTableInfo() {
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(
				new MybatisConfiguration(),
				UserAddressMapper.class.getName());
		assistant.setCurrentNamespace(UserAddressMapper.class.getName());
		TableInfoHelper.initTableInfo(assistant, UserAddress.class);
	}

	@Mock
	private UserAddressMapper userAddressMapper;

	@Mock
	private CurrentUser currentUser;

	@InjectMocks
	private AddressService addressService;

	@Test
	void shouldMakeFirstAddressDefaultAndBindCurrentUser() {
		when(currentUser.getUserId()).thenReturn(7L);
		when(userAddressMapper.selectCount(
				ArgumentMatchers.<LambdaQueryWrapper<UserAddress>>any()))
				.thenReturn(0L);
		when(userAddressMapper.insert(any(UserAddress.class))).thenReturn(1);

		UserAddress result = addressService.create(request(0));

		ArgumentCaptor<UserAddress> addressCaptor =
				ArgumentCaptor.forClass(UserAddress.class);
		verify(userAddressMapper).insert(addressCaptor.capture());
		UserAddress insertedAddress = addressCaptor.getValue();
		assertSame(insertedAddress, result);
		assertEquals(7L, insertedAddress.getUserId());
		assertEquals(1, insertedAddress.getIsDefault());
		assertEquals("张三", insertedAddress.getReceiverName());
		assertEquals("13800000000", insertedAddress.getPhone());
		assertEquals("浦东新区 1 号", insertedAddress.getDetailAddress());
		verify(userAddressMapper, never()).update(
				isNull(),
				ArgumentMatchers.<LambdaUpdateWrapper<UserAddress>>any());
	}

	@Test
	@SuppressWarnings("unchecked")
	void shouldListOnlyAddressesOwnedByCurrentUser() {
		when(currentUser.getUserId()).thenReturn(7L);
		when(userAddressMapper.selectList(
				ArgumentMatchers.<LambdaQueryWrapper<UserAddress>>any()))
				.thenReturn(List.of(address(10L, 7L, 1)));

		List<UserAddress> result = addressService.list();

		assertEquals(1, result.size());
		ArgumentCaptor<LambdaQueryWrapper<UserAddress>> wrapperCaptor =
				ArgumentCaptor.forClass(LambdaQueryWrapper.class);
		verify(userAddressMapper).selectList(wrapperCaptor.capture());
		assertTrue(wrapperCaptor.getValue()
				.getSqlSegment()
				.contains("user_id"));
		verify(currentUser).getUserId();
	}

	@Test
	void shouldKeepSubsequentAddressNonDefaultWhenNotRequested() {
		when(currentUser.getUserId()).thenReturn(7L);
		when(userAddressMapper.selectCount(
				ArgumentMatchers.<LambdaQueryWrapper<UserAddress>>any()))
				.thenReturn(1L);
		when(userAddressMapper.insert(any(UserAddress.class))).thenReturn(1);

		UserAddress result = addressService.create(request(0));

		assertEquals(0, result.getIsDefault());
		verify(userAddressMapper, never()).update(
				isNull(),
				ArgumentMatchers.<LambdaUpdateWrapper<UserAddress>>any());
	}

	@Test
	void shouldClearOldDefaultWhenNewAddressIsDefault() {
		when(currentUser.getUserId()).thenReturn(7L);
		when(userAddressMapper.selectCount(
				ArgumentMatchers.<LambdaQueryWrapper<UserAddress>>any()))
				.thenReturn(2L);
		when(userAddressMapper.update(
				isNull(),
				ArgumentMatchers.<LambdaUpdateWrapper<UserAddress>>any()))
				.thenReturn(1);
		when(userAddressMapper.insert(any(UserAddress.class))).thenReturn(1);

		UserAddress result = addressService.create(request(1));

		assertEquals(1, result.getIsDefault());
		verify(userAddressMapper).update(
				isNull(),
				ArgumentMatchers.<LambdaUpdateWrapper<UserAddress>>any());
	}

	@Test
	void shouldReportDefaultAddressConflict() {
		when(currentUser.getUserId()).thenReturn(7L);
		when(userAddressMapper.selectCount(
				ArgumentMatchers.<LambdaQueryWrapper<UserAddress>>any()))
				.thenReturn(0L);
		when(userAddressMapper.insert(any(UserAddress.class)))
				.thenThrow(new DuplicateKeyException("duplicate default address"));

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> addressService.create(request(0)));

		assertEquals(ErrorCode.BUSINESS_CONFLICT, exception.getErrorCode());
		assertEquals("默认地址设置冲突，请重试", exception.getMessage());
	}

	@Test
	void shouldUpdateAddressOwnedByCurrentUser() {
		when(currentUser.getUserId()).thenReturn(7L);
		when(userAddressMapper.selectOne(
				ArgumentMatchers.<LambdaQueryWrapper<UserAddress>>any()))
				.thenReturn(address(10L, 7L, 0));
		when(userAddressMapper.update(
				isNull(),
				ArgumentMatchers.<LambdaUpdateWrapper<UserAddress>>any()))
				.thenReturn(1);

		UserAddress result = addressService.update(10L, updateRequest(0));

		assertEquals(10L, result.getId());
		assertEquals(7L, result.getUserId());
		assertEquals("李四", result.getReceiverName());
		assertEquals("13900000000", result.getPhone());
		assertEquals("静安区 2 号", result.getDetailAddress());
		assertEquals(0, result.getIsDefault());
		verify(userAddressMapper).update(
				isNull(),
				ArgumentMatchers.<LambdaUpdateWrapper<UserAddress>>any());
	}

	@Test
	void shouldRejectUpdatingAddressOwnedByAnotherUser() {
		when(currentUser.getUserId()).thenReturn(7L);
		when(userAddressMapper.selectOne(
				ArgumentMatchers.<LambdaQueryWrapper<UserAddress>>any()))
				.thenReturn(null);

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> addressService.update(10L, updateRequest(0)));

		assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
		assertEquals("地址不存在", exception.getMessage());
		verify(userAddressMapper, never()).update(
				isNull(),
				ArgumentMatchers.<LambdaUpdateWrapper<UserAddress>>any());
	}

	@Test
	void shouldClearOldDefaultWhenUpdatedAddressBecomesDefault() {
		when(currentUser.getUserId()).thenReturn(7L);
		when(userAddressMapper.selectOne(
				ArgumentMatchers.<LambdaQueryWrapper<UserAddress>>any()))
				.thenReturn(address(10L, 7L, 0));
		when(userAddressMapper.update(
				isNull(),
				ArgumentMatchers.<LambdaUpdateWrapper<UserAddress>>any()))
				.thenReturn(1);

		UserAddress result = addressService.update(10L, updateRequest(1));

		assertEquals(1, result.getIsDefault());
		verify(userAddressMapper, times(2)).update(
				isNull(),
				ArgumentMatchers.<LambdaUpdateWrapper<UserAddress>>any());
	}

	@Test
	void shouldRejectUpdateWhenAddressDisappearsBeforeWrite() {
		when(currentUser.getUserId()).thenReturn(7L);
		when(userAddressMapper.selectOne(
				ArgumentMatchers.<LambdaQueryWrapper<UserAddress>>any()))
				.thenReturn(address(10L, 7L, 0));
		when(userAddressMapper.update(
				isNull(),
				ArgumentMatchers.<LambdaUpdateWrapper<UserAddress>>any()))
				.thenReturn(0);

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> addressService.update(10L, updateRequest(0)));

		assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
		assertEquals("地址不存在", exception.getMessage());
	}

	@Test
	@SuppressWarnings("unchecked")
	void shouldSetOwnedAddressAsDefaultInTwoUpdates() {
		when(currentUser.getUserId()).thenReturn(7L);
		when(userAddressMapper.selectOne(
				ArgumentMatchers.<LambdaQueryWrapper<UserAddress>>any()))
				.thenReturn(address(10L, 7L, 0));
		when(userAddressMapper.update(
				isNull(),
				ArgumentMatchers.<LambdaUpdateWrapper<UserAddress>>any()))
				.thenReturn(1);

		UserAddress result = addressService.setDefault(10L);

		assertEquals(1, result.getIsDefault());
		ArgumentCaptor<LambdaUpdateWrapper<UserAddress>> wrapperCaptor =
				ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
		verify(userAddressMapper, times(2)).update(
				isNull(), wrapperCaptor.capture());
		List<LambdaUpdateWrapper<UserAddress>> wrappers =
				wrapperCaptor.getAllValues();
		assertTrue(wrappers.get(0).getParamNameValuePairs().containsValue(0));
		assertTrue(wrappers.get(1).getParamNameValuePairs().containsValue(1));
	}

	@Test
	void shouldKeepSetDefaultIdempotent() {
		UserAddress defaultAddress = address(10L, 7L, 1);
		when(currentUser.getUserId()).thenReturn(7L);
		when(userAddressMapper.selectOne(
				ArgumentMatchers.<LambdaQueryWrapper<UserAddress>>any()))
				.thenReturn(defaultAddress);

		UserAddress result = addressService.setDefault(10L);

		assertSame(defaultAddress, result);
		verify(userAddressMapper, never()).update(
				isNull(),
				ArgumentMatchers.<LambdaUpdateWrapper<UserAddress>>any());
	}

	@Test
	void shouldRejectSettingAnotherUsersAddressAsDefault() {
		when(currentUser.getUserId()).thenReturn(7L);
		when(userAddressMapper.selectOne(
				ArgumentMatchers.<LambdaQueryWrapper<UserAddress>>any()))
				.thenReturn(null);

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> addressService.setDefault(10L));

		assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
		assertEquals("地址不存在", exception.getMessage());
		verify(userAddressMapper, never()).update(
				isNull(),
				ArgumentMatchers.<LambdaUpdateWrapper<UserAddress>>any());
	}

	@Test
	void shouldRejectSetDefaultWhenAddressDisappearsBeforeWrite() {
		when(currentUser.getUserId()).thenReturn(7L);
		when(userAddressMapper.selectOne(
				ArgumentMatchers.<LambdaQueryWrapper<UserAddress>>any()))
				.thenReturn(address(10L, 7L, 0));
		when(userAddressMapper.update(
				isNull(),
				ArgumentMatchers.<LambdaUpdateWrapper<UserAddress>>any()))
				.thenReturn(1, 0);

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> addressService.setDefault(10L));

		assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
		assertEquals("地址不存在", exception.getMessage());
	}

	@Test
	void shouldDeleteAddressOwnedByCurrentUser() {
		when(currentUser.getUserId()).thenReturn(7L);
		when(userAddressMapper.selectOne(
				ArgumentMatchers.<LambdaQueryWrapper<UserAddress>>any()))
				.thenReturn(address(10L, 7L, 1));
		when(userAddressMapper.delete(
				ArgumentMatchers.<LambdaQueryWrapper<UserAddress>>any()))
				.thenReturn(1);

		addressService.delete(10L);

		verify(userAddressMapper).delete(
				ArgumentMatchers.<LambdaQueryWrapper<UserAddress>>any());
	}

	@Test
	void shouldRejectDeletingAnotherUsersAddress() {
		when(currentUser.getUserId()).thenReturn(7L);
		when(userAddressMapper.selectOne(
				ArgumentMatchers.<LambdaQueryWrapper<UserAddress>>any()))
				.thenReturn(null);

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> addressService.delete(10L));

		assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
		assertEquals("地址不存在", exception.getMessage());
		verify(userAddressMapper, never()).delete(
				ArgumentMatchers.<LambdaQueryWrapper<UserAddress>>any());
	}

	@Test
	void shouldRejectDeleteWhenAddressDisappearsBeforeDelete() {
		when(currentUser.getUserId()).thenReturn(7L);
		when(userAddressMapper.selectOne(
				ArgumentMatchers.<LambdaQueryWrapper<UserAddress>>any()))
				.thenReturn(address(10L, 7L, 0));
		when(userAddressMapper.delete(
				ArgumentMatchers.<LambdaQueryWrapper<UserAddress>>any()))
				.thenReturn(0);

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> addressService.delete(10L));

		assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
		assertEquals("地址不存在", exception.getMessage());
	}

	private AddressCreateRequest request(Integer isDefault) {
		AddressCreateRequest request = new AddressCreateRequest();
		request.setReceiverName("  张三  ");
		request.setPhone("  13800000000  ");
		request.setProvince("  上海市  ");
		request.setCity("  上海市  ");
		request.setDistrict("  浦东新区  ");
		request.setDetailAddress("  浦东新区 1 号  ");
		request.setIsDefault(isDefault);
		return request;
	}

	private AddressUpdateRequest updateRequest(Integer isDefault) {
		AddressUpdateRequest request = new AddressUpdateRequest();
		request.setReceiverName("  李四  ");
		request.setPhone("  13900000000  ");
		request.setProvince("  上海市  ");
		request.setCity("  上海市  ");
		request.setDistrict("  静安区  ");
		request.setDetailAddress("  静安区 2 号  ");
		request.setIsDefault(isDefault);
		return request;
	}

	private UserAddress address(Long id, Long userId, Integer isDefault) {
		UserAddress address = new UserAddress();
		address.setId(id);
		address.setUserId(userId);
		address.setReceiverName("原收货人");
		address.setPhone("13800000000");
		address.setProvince("上海市");
		address.setCity("上海市");
		address.setDistrict("浦东新区");
		address.setDetailAddress("原地址");
		address.setIsDefault(isDefault);
		return address;
	}
}
