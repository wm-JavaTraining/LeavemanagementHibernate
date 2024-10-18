package com.wavemaker.leavemanagement.service.impl;

import com.wavemaker.leavemanagement.repository.EmployeeNumberOfLeavesRepository;
import com.wavemaker.leavemanagement.repository.impl.indb.EmployeeNumberOfLeavesRepositoryImpl;
import com.wavemaker.leavemanagement.service.EmployeeNumberOfLeavesService;

public class EmployeeNumberOfLeavesServiceImpl implements EmployeeNumberOfLeavesService {
    private final EmployeeNumberOfLeavesRepository employeeNumberOfLeavesRepository;

    public EmployeeNumberOfLeavesServiceImpl() {
        this.employeeNumberOfLeavesRepository = new EmployeeNumberOfLeavesRepositoryImpl();
    }

    @Override
    public int getNoOfLeaves(int employeeId) {
        return employeeNumberOfLeavesRepository.getNoOfLeaves(employeeId);
    }
}
