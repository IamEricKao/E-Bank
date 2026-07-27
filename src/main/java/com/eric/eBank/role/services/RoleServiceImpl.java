package com.eric.eBank.role.services;

import com.eric.eBank.exceptions.BadRequestException;
import com.eric.eBank.exceptions.NotFoundException;
import com.eric.eBank.res.Response;
import com.eric.eBank.role.entity.Role;
import com.eric.eBank.role.repo.RoleRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepo roleRepo;

    @Override
    public Response<Role> createRole(Role roleRequest) {

        if (roleRepo.findByName(roleRequest.getName()).isPresent()) {
            throw new BadRequestException("角色重複");
        }

        Role savedRole = roleRepo.save(roleRequest);

        return Response.<Role>builder()
                .statusCode(HttpStatus.OK.value())
                .message("角色創建成功")
                .data(savedRole)
                .build();
    }

    @Override
    public Response<Role> updateRole(Role roleRequest) {

        Role role = roleRepo.findById(roleRequest.getId())
                .orElseThrow(() -> new NotFoundException("角色不存在"));

        role.setName(roleRequest.getName());
        Role updatedRole = roleRepo.save(role);

        return Response.<Role>builder()
                .statusCode(HttpStatus.OK.value())
                .message("角色更新成功")
                .data(updatedRole)
                .build();
    }

    @Override
    public Response<List<Role>> getAllRole() {

        List<Role> allRoles = roleRepo.findAll().stream()
                .sorted(Comparator.comparing(Role::getId))
                .toList();

        return Response.<List<Role>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("查詢成功")
                .data(allRoles)
                .build();
    }

    @Override
    public Response<?> deleteRole(Long id) {

        if (!roleRepo.existsById(id)) {
            throw new NotFoundException("角色不存在");
        }

        roleRepo.deleteById(id);

        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("角色刪除成功")
                .build();
    }
}
