package com.tuttime.user.mapper;

import com.tuttime.user.dto.UserResponse;
import com.tuttime.user.entity.User;
import com.tuttime.user.util.DateMapper;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
		uses = DateMapper.class,
		unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

	UserResponse toResponse(User entity);
}