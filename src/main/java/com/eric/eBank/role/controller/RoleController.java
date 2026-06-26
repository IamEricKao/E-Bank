package com.eric.eBank.role.controller;

import com.eric.eBank.res.Response;
import com.eric.eBank.role.entity.Role;
import com.eric.eBank.role.services.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
//@PreAuthorize("hasAuthority('ADMIN')")
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    public ResponseEntity<Response<Role>> createRole(@RequestBody Role roleRequest){
        return ResponseEntity.ok(roleService.createRole(roleRequest));
    }

    @PutMapping
    public ResponseEntity<Response<Role>> updateRole(@RequestBody Role roleRequest){
        return ResponseEntity.ok(roleService.updateRole(roleRequest));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRole(@PathVariable Long id){
        return ResponseEntity.ok(roleService.deleteRole(id));
    }

    @GetMapping()
    public ResponseEntity<?> getAllRole(){
        return ResponseEntity.ok(roleService.getAllRole());
    }
}
