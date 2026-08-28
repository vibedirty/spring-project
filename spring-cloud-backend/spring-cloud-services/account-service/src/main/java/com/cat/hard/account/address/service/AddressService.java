package com.cat.hard.account.address.service;

import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cat.hard.account.address.dto.AddressCreateRequest;
import com.cat.hard.account.address.dto.AddressUpdateRequest;
import com.cat.hard.account.address.entity.UserAddress;
import com.cat.hard.account.address.mapper.UserAddressMapper;
import com.cat.hard.account.auth.security.CurrentUser;
import com.cat.hard.account.common.error.ErrorCode;
import com.cat.hard.account.common.exception.BusinessException;

import jakarta.annotation.Resource;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AddressService {

	@Resource
	private UserAddressMapper userAddressMapper;

	@Resource
	private CurrentUser currentUser;

	@Transactional
	public UserAddress create(AddressCreateRequest request) {
		Long userId = currentUser.getUserId();
		LambdaQueryWrapper<UserAddress> countWrapper =
				new LambdaQueryWrapper<UserAddress>(UserAddress.class);
		countWrapper.eq(UserAddress::getUserId, userId);
		boolean firstAddress = userAddressMapper.selectCount(countWrapper) == 0;
		boolean makeDefault = firstAddress
				|| Integer.valueOf(1).equals(request.getIsDefault());

		if (makeDefault && !firstAddress) {
			clearCurrentDefault(userId);
		}

		UserAddress address = new UserAddress();
		address.setUserId(userId);
		address.setReceiverName(request.getReceiverName().trim());
		address.setPhone(request.getPhone().trim());
		address.setProvince(request.getProvince().trim());
		address.setCity(request.getCity().trim());
		address.setDistrict(request.getDistrict().trim());
		address.setDetailAddress(request.getDetailAddress().trim());
		address.setIsDefault(makeDefault ? 1 : 0);

		try {
			userAddressMapper.insert(address);
		}
		catch (DuplicateKeyException exception) {
			throw new BusinessException(
					ErrorCode.BUSINESS_CONFLICT,
					"默认地址设置冲突，请重试");
		}

		return address;
	}

	public List<UserAddress> list() {
		Long userId = currentUser.getUserId();
		LambdaQueryWrapper<UserAddress> queryWrapper =
				new LambdaQueryWrapper<UserAddress>(UserAddress.class);
		queryWrapper.eq(UserAddress::getUserId, userId)
				.orderByDesc(UserAddress::getIsDefault)
				.orderByDesc(UserAddress::getId);
		return userAddressMapper.selectList(queryWrapper);
	}

	public UserAddress getOwnedAddress(Long id) {
		return getOwnedAddress(id, currentUser.getUserId());
	}

	@Transactional
	public UserAddress update(Long id, AddressUpdateRequest request) {
		Long userId = currentUser.getUserId();
		UserAddress currentAddress = getOwnedAddress(id, userId);

		boolean makeDefault = Integer.valueOf(1).equals(request.getIsDefault());
		if (makeDefault && !Integer.valueOf(1).equals(currentAddress.getIsDefault())) {
			clearCurrentDefault(userId);
		}

		String receiverName = request.getReceiverName().trim();
		String phone = request.getPhone().trim();
		String province = request.getProvince().trim();
		String city = request.getCity().trim();
		String district = request.getDistrict().trim();
		String detailAddress = request.getDetailAddress().trim();
		int isDefault = makeDefault ? 1 : 0;
		LocalDateTime updatedAt = LocalDateTime.now();

		LambdaUpdateWrapper<UserAddress> updateWrapper =
				new LambdaUpdateWrapper<UserAddress>(UserAddress.class);
		updateWrapper.eq(UserAddress::getId, id)
				.eq(UserAddress::getUserId, userId)
				.set(UserAddress::getReceiverName, receiverName)
				.set(UserAddress::getPhone, phone)
				.set(UserAddress::getProvince, province)
				.set(UserAddress::getCity, city)
				.set(UserAddress::getDistrict, district)
				.set(UserAddress::getDetailAddress, detailAddress)
				.set(UserAddress::getIsDefault, isDefault)
				.set(UserAddress::getUpdatedAt, updatedAt);

		try {
			int affectedRows = userAddressMapper.update(null, updateWrapper);
			if (affectedRows == 0) {
				throw new BusinessException(
						ErrorCode.RESOURCE_NOT_FOUND,
						"地址不存在");
			}
		}
		catch (DuplicateKeyException exception) {
			throw new BusinessException(
					ErrorCode.BUSINESS_CONFLICT,
					"默认地址设置冲突，请重试");
		}

		currentAddress.setReceiverName(receiverName);
		currentAddress.setPhone(phone);
		currentAddress.setProvince(province);
		currentAddress.setCity(city);
		currentAddress.setDistrict(district);
		currentAddress.setDetailAddress(detailAddress);
		currentAddress.setIsDefault(isDefault);
		currentAddress.setUpdatedAt(updatedAt);
		return currentAddress;
	}

	@Transactional
	public UserAddress setDefault(Long id) {
		Long userId = currentUser.getUserId();
		UserAddress currentAddress = getOwnedAddress(id, userId);
		if (Integer.valueOf(1).equals(currentAddress.getIsDefault())) {
			return currentAddress;
		}

		clearCurrentDefault(userId);
		LocalDateTime updatedAt = LocalDateTime.now();
		LambdaUpdateWrapper<UserAddress> updateWrapper =
				new LambdaUpdateWrapper<UserAddress>(UserAddress.class);
		updateWrapper.eq(UserAddress::getId, id)
				.eq(UserAddress::getUserId, userId)
				.set(UserAddress::getIsDefault, 1)
				.set(UserAddress::getUpdatedAt, updatedAt);

		try {
			int affectedRows = userAddressMapper.update(null, updateWrapper);
			if (affectedRows == 0) {
				throw new BusinessException(
						ErrorCode.RESOURCE_NOT_FOUND,
						"地址不存在");
			}
		}
		catch (DuplicateKeyException exception) {
			throw new BusinessException(
					ErrorCode.BUSINESS_CONFLICT,
					"默认地址设置冲突，请重试");
		}

		currentAddress.setIsDefault(1);
		currentAddress.setUpdatedAt(updatedAt);
		return currentAddress;
	}

	@Transactional
	public void delete(Long id) {
		Long userId = currentUser.getUserId();
		getOwnedAddress(id, userId);

		LambdaQueryWrapper<UserAddress> deleteWrapper =
				new LambdaQueryWrapper<UserAddress>(UserAddress.class);
		deleteWrapper.eq(UserAddress::getId, id)
				.eq(UserAddress::getUserId, userId);
		int affectedRows = userAddressMapper.delete(deleteWrapper);
		if (affectedRows == 0) {
			throw new BusinessException(
					ErrorCode.RESOURCE_NOT_FOUND,
					"地址不存在");
		}
	}

	private UserAddress getOwnedAddress(Long id, Long userId) {
		LambdaQueryWrapper<UserAddress> queryWrapper =
				new LambdaQueryWrapper<UserAddress>(UserAddress.class);
		queryWrapper.eq(UserAddress::getId, id)
				.eq(UserAddress::getUserId, userId);
		UserAddress address = userAddressMapper.selectOne(queryWrapper);
		if (address == null) {
			throw new BusinessException(
					ErrorCode.RESOURCE_NOT_FOUND,
					"地址不存在");
		}
		return address;
	}

	private void clearCurrentDefault(Long userId) {
		LambdaUpdateWrapper<UserAddress> updateWrapper =
				new LambdaUpdateWrapper<UserAddress>(UserAddress.class);
		updateWrapper.eq(UserAddress::getUserId, userId)
				.eq(UserAddress::getIsDefault, 1)
				.set(UserAddress::getIsDefault, 0)
				.set(UserAddress::getUpdatedAt, LocalDateTime.now());
		userAddressMapper.update(null, updateWrapper);
	}
}
