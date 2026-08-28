package com.cat.hard.account.address.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AddressUpdateRequest {

	@NotBlank(message = "收货人姓名不能为空")
	@Size(max = 32, message = "收货人姓名长度不能超过32个字符")
	private String receiverName;

	@NotBlank(message = "收货人手机号不能为空")
	@Size(max = 20, message = "收货人手机号长度不能超过20个字符")
	private String phone;

	@NotBlank(message = "省/直辖市不能为空")
	@Size(max = 64, message = "省/直辖市长度不能超过64个字符")
	private String province;

	@NotBlank(message = "城市不能为空")
	@Size(max = 64, message = "城市长度不能超过64个字符")
	private String city;

	@NotBlank(message = "区/县不能为空")
	@Size(max = 64, message = "区/县长度不能超过64个字符")
	private String district;

	@NotBlank(message = "详细地址不能为空")
	@Size(max = 255, message = "详细地址长度不能超过255个字符")
	private String detailAddress;

	@NotNull(message = "是否默认状态不能为空")
	@Min(value = 0, message = "是否默认状态值不合法")
	@Max(value = 1, message = "是否默认状态值不合法")
	private Integer isDefault;

	public String getReceiverName() {
		return receiverName;
	}

	public void setReceiverName(String receiverName) {
		this.receiverName = receiverName;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getProvince() {
		return province;
	}

	public void setProvince(String province) {
		this.province = province;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getDistrict() {
		return district;
	}

	public void setDistrict(String district) {
		this.district = district;
	}

	public String getDetailAddress() {
		return detailAddress;
	}

	public void setDetailAddress(String detailAddress) {
		this.detailAddress = detailAddress;
	}

	public Integer getIsDefault() {
		return isDefault;
	}

	public void setIsDefault(Integer isDefault) {
		this.isDefault = isDefault;
	}
}
