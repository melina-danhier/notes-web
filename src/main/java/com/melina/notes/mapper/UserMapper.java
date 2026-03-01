package com.melina.notes.mapper;

import com.melina.notes.dto.UpdateUserDTO;
import com.melina.notes.dto.UserDTO;
import com.melina.notes.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDTO toDto(User user);
    void update(@MappingTarget User user, UpdateUserDTO userDto);
}
