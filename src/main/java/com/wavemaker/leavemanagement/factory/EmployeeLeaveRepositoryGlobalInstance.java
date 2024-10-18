package com.wavemaker.leavemanagement.factory;

import com.wavemaker.leavemanagement.repository.EmployeeLeaveRepository;
import com.wavemaker.leavemanagement.repository.impl.indb.EmployeeLeaveRepositoryImpl;

public class EmployeeLeaveRepositoryGlobalInstance {
    private static EmployeeLeaveRepository employeeLeaveRepository = null;

    public static EmployeeLeaveRepository getEmployeeLeaveRepositoryInstance() {
        if (employeeLeaveRepository == null) {
            synchronized (EmployeeLeaveRepositoryGlobalInstance.class) {
                if (employeeLeaveRepository == null) {
                    employeeLeaveRepository = new EmployeeLeaveRepositoryImpl();
                }
            }
        }
        return employeeLeaveRepository;
    }

}
