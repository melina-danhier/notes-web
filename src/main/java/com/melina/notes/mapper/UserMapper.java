package com.melina.notes.mapper;

import com.melina.notes.dto.UpdateUserDTO;
import com.melina.notes.dto.UserDTO;
import com.melina.notes.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = NoteMapper.class)
public interface UserMapper {
    UserDTO toDto(User user);

    @Mapping(target = "role", ignore = true)
    @Mapping(target = "id", ignore = true)
    void update(@MappingTarget User user, UpdateUserDTO userDto);
}
