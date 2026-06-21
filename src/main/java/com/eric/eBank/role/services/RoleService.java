package com.eric.eBank.role.services;

import com.eric.eBank.res.Response;
import com.eric.eBank.role.entity.Role;

import java.util.List;

public interface RoleService {
    Response<Role> createRole(Role roleRequest);

    Response<Role> updateRole(Role roleRequest);

    Response<List<Role>> getAllRole();

    Response<?> deleteRole(Long id);
}
